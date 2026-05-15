package com.backend.features.sms.service.impl;

import com.backend.config.MobileBankingConfig;
import com.backend.features.notification.models.NotificationConfig;
import com.backend.features.notification.repository.NotificationConfigRepository;
import com.backend.features.sms.dto.OracleSmsDto;
import com.backend.features.sms.dto.SendBatchSmsResponse;
import com.backend.features.sms.dto.SendSmsRequest;
import com.backend.features.sms.dto.SendSmsResponse;
import com.backend.features.sms.enums.SmsStatus;
import com.backend.features.sms.helper.OracleSmsHelper;
import com.backend.features.sms.helper.PhoneValidator;
import com.backend.features.sms.models.SmsLog;
import com.backend.features.sms.repository.SmsRepository;
import com.backend.features.sms.service.BatchProcessingStatusService;
import com.backend.features.sms.service.SmsService;
import com.backend.shared.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {

    private static final String SMS_CONFIG_TYPE = "NOTIFICATION_SMS";

    private final SmsRepository smsRepository;
    private final MobileBankingConfig mobileBankingConfig;
    private final HttpClientUtil httpClientUtil;
    private final OracleSmsHelper oracleSmsHelper;
    private final NotificationConfigRepository notificationConfigRepository;
    private final PhoneValidator phoneValidator;
    private final BatchProcessingStatusService batchProcessingStatusService;

    @Override
    @Transactional
    public SendSmsResponse sendSms(SendSmsRequest request) {
        String requestId = String.valueOf(System.currentTimeMillis());
        String phone = request.getPhone();
        String content = request.getContent();

        log.info("Sending single SMS - requestId: {}, phone: {}", requestId, phone);

        // Validate phone number
        if (!phoneValidator.isValidPhone(phone)) {
            String errorMsg = phoneValidator.getErrorMessage(phone);
            log.error("Invalid phone number - requestId: {}, error: {}", requestId, errorMsg);
            logSmsToPostgres(requestId, phone, content, SmsStatus.ERROR, errorMsg);

            return SendSmsResponse.builder()
                    .requestId(requestId)
                    .phone(phone)
                    .status("ERROR")
                    .sentAt(LocalDateTime.now())
                    .error(errorMsg)
                    .build();
        }

        // Format phone number
        String formattedPhone = phoneValidator.formatPhoneNumber(phone);
        log.debug("Formatted phone number: {} -> {}", phone, formattedPhone);

        try {
            boolean success = sendSoapSms(formattedPhone, content, requestId);

            if (success) {
                logSmsToPostgres(requestId, formattedPhone, content, SmsStatus.SUCCESS, null);
                log.info("SMS sent successfully - requestId: {}, phone: {}", requestId, formattedPhone);

                return SendSmsResponse.builder()
                        .requestId(requestId)
                        .phone(formattedPhone)
                        .status("SUCCESS")
                        .sentAt(LocalDateTime.now())
                        .build();
            } else {
                logSmsToPostgres(requestId, formattedPhone, content, SmsStatus.ERROR, "SOAP response error");
                log.warn("SMS send failed - requestId: {}, phone: {}", requestId, formattedPhone);

                return SendSmsResponse.builder()
                        .requestId(requestId)
                        .phone(formattedPhone)
                        .status("ERROR")
                        .sentAt(LocalDateTime.now())
                        .error("SOAP response error")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error sending SMS - requestId: {}, phone: {}", requestId, formattedPhone, e);
            logSmsToPostgres(requestId, formattedPhone, content, SmsStatus.ERROR, e.getMessage());

            return SendSmsResponse.builder()
                    .requestId(requestId)
                    .phone(formattedPhone)
                    .status("ERROR")
                    .sentAt(LocalDateTime.now())
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public SendBatchSmsResponse startBatchProcessing() {
        log.info("API: Initiating batch SMS processing");

        try {
            List<OracleSmsDto> processingRecords = oracleSmsHelper.selectProcessingSmsRecords();
            int totalRecords = processingRecords.size();

            log.info("Found {} PROCESSING SMS records from Oracle", totalRecords);

            batchProcessingStatusService.startProcessing(totalRecords);
            processSms();

            return SendBatchSmsResponse.builder()
                    .totalProcessed(totalRecords)
                    .successCount(0)
                    .failureCount(0)
                    .message("SMS batch processing started. Total: " + totalRecords + " records")
                    .build();
        } catch (Exception e) {
            log.error("Failed to start batch processing", e);
            batchProcessingStatusService.failProcessing(e.getMessage());
            return SendBatchSmsResponse.builder()
                    .totalProcessed(0)
                    .successCount(0)
                    .failureCount(0)
                    .message("Failed to start batch processing: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<SendBatchSmsResponse> processSms() {
        log.info("Starting async batch SMS processing from Oracle D_CBS_SMS_LOG");

        try {
            NotificationConfig smsConfig = notificationConfigRepository.findActiveByConfigType(SMS_CONFIG_TYPE)
                    .orElseThrow(() -> new RuntimeException("SMS configuration not found"));

            String smsMessage = smsConfig.getConfigValue();
            log.info("Using SMS template: {}", smsConfig.getDescription());

            List<OracleSmsDto> processingRecords = oracleSmsHelper.selectProcessingSmsRecords();
            log.info("Processing {} PROCESSING SMS records from Oracle", processingRecords.size());

            int successCount = 0;
            int failureCount = 0;

            for (OracleSmsDto record : processingRecords) {
                String requestId = String.valueOf(System.currentTimeMillis());
                try {
                    // Validate phone number
                    if (!phoneValidator.isValidPhone(record.getPhone())) {
                        String errorMsg = phoneValidator.getErrorMessage(record.getPhone());
                        log.error("Invalid phone number - msgId: {}, requestId: {}, phone: {}",
                                record.getMsgId(), requestId, record.getPhone());
                        oracleSmsHelper.updateSmsStatus(record.getMsgId(), "ERROR");
                        logSmsToPostgres(record.getMsgId(), record.getPhone(), smsMessage, SmsStatus.ERROR, errorMsg);
                        batchProcessingStatusService.updateProgress(false);
                        failureCount++;
                        continue;
                    }

                    // Format phone number
                    String formattedPhone = phoneValidator.formatPhoneNumber(record.getPhone());
                    log.debug("Formatted phone number: {} -> {}", record.getPhone(), formattedPhone);

                    boolean success = sendSoapSms(formattedPhone, smsMessage, record.getMsgId());
                    if (success) {
                        oracleSmsHelper.updateSmsStatus(record.getMsgId(), "SUCCESS");
                        logSmsToPostgres(record.getMsgId(), formattedPhone, smsMessage, SmsStatus.SUCCESS, null);
                        log.info("SMS sent successfully - msgId: {}, requestId: {}, phone: {}",
                                record.getMsgId(), requestId, formattedPhone);
                        batchProcessingStatusService.updateProgress(true);
                        successCount++;
                    } else {
                        oracleSmsHelper.updateSmsStatus(record.getMsgId(), "ERROR");
                        logSmsToPostgres(record.getMsgId(), formattedPhone, smsMessage, SmsStatus.ERROR, "SOAP response error");
                        log.warn("SMS send failed - msgId: {}, requestId: {}, phone: {}",
                                record.getMsgId(), requestId, formattedPhone);
                        batchProcessingStatusService.updateProgress(false);
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Error sending SMS - msgId: {}, requestId: {}, phone: {}",
                            record.getMsgId(), requestId, record.getPhone(), e);
                    oracleSmsHelper.updateSmsStatus(record.getMsgId(), "ERROR");
                    logSmsToPostgres(record.getMsgId(), record.getPhone(), smsMessage, SmsStatus.ERROR, e.getMessage());
                    batchProcessingStatusService.updateProgress(false);
                    failureCount++;
                }
            }

            int totalProcessed = processingRecords.size();
            log.info("Batch SMS processing completed - Total: {}, Success: {}, Failure: {}", totalProcessed, successCount, failureCount);

            batchProcessingStatusService.completeProcessing();

            return CompletableFuture.completedFuture(SendBatchSmsResponse.builder()
                    .totalProcessed(totalProcessed)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .message("SMS batch processing completed")
                    .build());
        } catch (Exception e) {
            log.error("Batch SMS processing failed", e);
            batchProcessingStatusService.failProcessing(e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private boolean sendSoapSms(String phone, String message, String msgId) {
        String otpUrl = mobileBankingConfig.getOtpUrl();
        String secretKey = mobileBankingConfig.getSecretKey();
        String requestId = String.valueOf(System.currentTimeMillis());

        String soapXml = buildSoapRequest(requestId, phone, message, secretKey);

        try {
            log.info("Sending SOAP SMS - msgId: {}, requestId: {}, phone: {}", msgId, requestId, phone);

            String responseXml = httpClientUtil.postForString(otpUrl, soapXml, "application/soap+xml");

            Matcher matcher = Pattern.compile("<(?:\\w+:)?return>(.*?)</(?:\\w+:)?return>").matcher(responseXml);
            String jsonPayload = matcher.find() ? matcher.group(1) : null;

            if (jsonPayload != null && jsonPayload.contains("\"rescode\":\"00\"")) {
                log.info("SMS sent successfully - msgId: {}, requestId: {}, phone: {}", msgId, requestId, phone);
                return true;
            } else {
                log.warn("SMS sending failed - msgId: {}, requestId: {}, response: {}", msgId, requestId, jsonPayload);
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending SOAP SMS - msgId: {}, requestId: {}, phone: {}", msgId, requestId, phone, e);
            return false;
        }
    }

    private String buildSoapRequest(String requestID, String phone, String message, String secretKey) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' "
                + "xmlns:cpb='http://cpbmobile.vnpay.vn'>"
                + "<soap:Header/>"
                + "<soap:Body>"
                + "<cpb:sendSmsNew>"
                + "<cpb:requestId>" + requestID + "</cpb:requestId>"
                + "<cpb:keyword>CPBSMS</cpb:keyword>"
                + "<cpb:mobileNo>" + phone + "</cpb:mobileNo>"
                + "<cpb:content><![CDATA[" + message + "]]></cpb:content>"
                + "<cpb:requestTime></cpb:requestTime>"
                + "<cpb:contentType>9</cpb:contentType>"
                + "<cpb:secretKey>" + secretKey + "</cpb:secretKey>"
                + "</cpb:sendSmsNew>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    private String escapeXml(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Log SMS to PostgreSQL audit table (never deleted - permanent history)
     */
    private void logSmsToPostgres(String msgId, String phone, String message, SmsStatus status, String errorDetails) {
        try {
            // Check if record already exists
            var existingSms = smsRepository.findByMsgId(msgId);

            if (existingSms.isPresent()) {
                // Update existing record
                SmsLog smsLog = existingSms.get();
                smsLog.setPhone(phone);
                smsLog.setSmsContent(message);
                smsLog.setSmsStatus(status);
                smsLog.setErrorDetails(errorDetails);
                smsLog.setSentAt(LocalDateTime.now());
                smsRepository.save(smsLog);
                log.debug("Updated SMS log in PostgreSQL - msgId: {}, status: {}", msgId, status.getValue());
            } else {
                // Create new record
                SmsLog smsLog = SmsLog.builder()
                        .msgId(msgId)
                        .phone(phone)
                        .smsContent(message)
                        .smsStatus(status)
                        .errorDetails(errorDetails)
                        .createdAt(LocalDateTime.now())
                        .sentAt(LocalDateTime.now())
                        .build();

                smsRepository.save(smsLog);
                log.debug("Created SMS log in PostgreSQL - msgId: {}, status: {}", msgId, status.getValue());
            }
        } catch (Exception e) {
            log.error("Error logging SMS to PostgreSQL - msgId: {}, error: {}", msgId, e.getMessage(), e);
        }
    }
}
