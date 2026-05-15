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

        if (!phoneValidator.isValidPhone(phone)) {
            String errorMsg = phoneValidator.getErrorMessage(phone);
            logSmsToPostgres(requestId, phone, content, SmsStatus.ERROR, errorMsg);

            return SendSmsResponse.builder()
                    .requestId(requestId)
                    .phone(phone)
                    .status("ERROR")
                    .sentAt(LocalDateTime.now())
                    .error(errorMsg)
                    .build();
        }

        String formattedPhone = phoneValidator.formatPhoneNumber(phone);

        try {
            boolean success = sendSoapSms(formattedPhone, content, requestId);

            if (success) {
                logSmsToPostgres(requestId, formattedPhone, content, SmsStatus.SUCCESS, null);
                return SendSmsResponse.builder()
                        .requestId(requestId)
                        .phone(formattedPhone)
                        .status("SUCCESS")
                        .sentAt(LocalDateTime.now())
                        .build();
            } else {
                logSmsToPostgres(requestId, formattedPhone, content, SmsStatus.ERROR, "SOAP response error");
                return SendSmsResponse.builder()
                        .requestId(requestId)
                        .phone(formattedPhone)
                        .status("ERROR")
                        .sentAt(LocalDateTime.now())
                        .error("SOAP response error")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error sending single SMS - requestId: {}, error: {}", requestId, e.getMessage());
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
        try {
            List<OracleSmsDto> processingRecords = oracleSmsHelper.selectProcessingSmsRecords();
            int totalRecords = processingRecords.size();

            batchProcessingStatusService.startProcessing(totalRecords);
            processSms();

            return SendBatchSmsResponse.builder()
                    .totalProcessed(totalRecords)
                    .successCount(0)
                    .failureCount(0)
                    .message("SMS batch processing started. Total: " + totalRecords + " records")
                    .build();
        } catch (Exception e) {
            log.error("Failed to start batch processing: {}", e.getMessage());
            batchProcessingStatusService.failProcessing(e.getMessage());
            return SendBatchSmsResponse.builder()
                    .totalProcessed(0)
                    .successCount(0)
                    .failureCount(0)
                    .message("Failed to start batch processing")
                    .build();
        }
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<SendBatchSmsResponse> processSms() {
        try {
            NotificationConfig smsConfig = notificationConfigRepository.findActiveByConfigType(SMS_CONFIG_TYPE)
                    .orElseThrow(() -> new RuntimeException("SMS configuration not found"));

            String smsMessage = smsConfig.getConfigValue();
            List<OracleSmsDto> processingRecords = oracleSmsHelper.selectProcessingSmsRecords();

            int successCount = 0;
            int failureCount = 0;

            for (OracleSmsDto record : processingRecords) {
                String requestId = String.valueOf(System.currentTimeMillis());
                try {
                    if (!phoneValidator.isValidPhone(record.getPhone())) {
                        String errorMsg = phoneValidator.getErrorMessage(record.getPhone());
                        oracleSmsHelper.updateSmsStatus(record.getMsgId(), "ERROR");
                        logSmsToPostgres(record.getMsgId(), record.getPhone(), smsMessage, SmsStatus.ERROR, errorMsg);
                        batchProcessingStatusService.updateProgress(false);
                        failureCount++;
                        continue;
                    }

                    String formattedPhone = phoneValidator.formatPhoneNumber(record.getPhone());
                    log.info("Processing SMS - msgId: {}, phone: {}, messageLength: {}, messagePreview: {}...",
                            record.getMsgId(), formattedPhone, smsMessage.length(),
                            smsMessage.length() > 50 ? smsMessage.substring(0, 50) : smsMessage);
                    boolean success = sendSoapSms(formattedPhone, smsMessage, record.getMsgId());

                    if (success) {
                        oracleSmsHelper.updateSmsStatus(record.getMsgId(), "SUCCESS");
                        logSmsToPostgres(record.getMsgId(), formattedPhone, smsMessage, SmsStatus.SUCCESS, null);
                        batchProcessingStatusService.updateProgress(true);
                        successCount++;
                    } else {
                        oracleSmsHelper.updateSmsStatus(record.getMsgId(), "ERROR");
                        logSmsToPostgres(record.getMsgId(), formattedPhone, smsMessage, SmsStatus.ERROR, "SOAP response error");
                        batchProcessingStatusService.updateProgress(false);
                        failureCount++;
                    }
                } catch (Exception e) {
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
            log.error("Batch SMS processing failed: {}", e.getMessage());
            batchProcessingStatusService.failProcessing(e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private boolean sendSoapSms(String phone, String message, String msgId) {
        String otpUrl = mobileBankingConfig.getOtpUrl();
        String secretKey = mobileBankingConfig.getSecretKey();
        String requestId = String.valueOf(System.currentTimeMillis());

        String soapXml = buildSoapRequest(requestId, phone, message, secretKey);

        log.debug("SOAP Request - msgId: {}, phone: {}, msgLength: {}, xml: {}",
                msgId, phone, message.length(), soapXml);

        try {
            String responseXml = httpClientUtil.postForString(otpUrl, soapXml, "application/soap+xml");

            Matcher matcher = Pattern.compile("<(?:\\w+:)?return>(.*?)</(?:\\w+:)?return>").matcher(responseXml);
            String jsonPayload = matcher.find() ? matcher.group(1) : null;

            if (jsonPayload != null && jsonPayload.contains("\"rescode\":\"00\"")) {
                return true;
            } else {
                String errorCode = "unknown";
                if (jsonPayload != null && jsonPayload.contains("\"rescode\"")) {
                    Matcher codeMatch = Pattern.compile("\"rescode\":\"(\\d+)\"").matcher(jsonPayload);
                    if (codeMatch.find()) {
                        errorCode = codeMatch.group(1);
                    }
                }
                log.warn("SMS gateway error - msgId: {}, phone: {}, rescode: {}, response: {}",
                        msgId, phone, errorCode, jsonPayload);
                return false;
            }

        } catch (Exception e) {
            log.error("SOAP SMS error - msgId: {}, phone: {}, error: {}", msgId, phone, e.getMessage());
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


    /**
     * Log SMS to PostgreSQL audit table (never deleted - permanent history)
     */
    private void logSmsToPostgres(String msgId, String phone, String message, SmsStatus status, String errorDetails) {
        try {
            var existingSms = smsRepository.findByMsgId(msgId);

            if (existingSms.isPresent()) {
                SmsLog smsLog = existingSms.get();
                smsLog.setPhone(phone);
                smsLog.setSmsContent(message);
                smsLog.setSmsStatus(status);
                smsLog.setErrorDetails(errorDetails);
                smsLog.setSentAt(LocalDateTime.now());
                smsRepository.save(smsLog);
            } else {
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
            }
        } catch (Exception e) {
            log.error("Error logging SMS - msgId: {}, error: {}", msgId, e.getMessage());
        }
    }
}
