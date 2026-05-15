package com.backend.features.sms_accepted.service.impl;

import com.backend.config.MobileBankingConfig;
import com.backend.features.notification.models.NotificationConfig;
import com.backend.features.notification.repository.NotificationConfigRepository;
import com.backend.features.sms_accepted.dto.OracleSmsDto;
import com.backend.features.sms_accepted.dto.SendBatchSmsResponse;
import com.backend.features.sms_accepted.helper.OracleSmsHelper;
import com.backend.features.sms_accepted.models.SmsAcceptedLog;
import com.backend.features.sms_accepted.repository.SmsAcceptedRepository;
import com.backend.features.sms_accepted.service.SmsAcceptedService;
import com.backend.shared.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsAcceptedServiceImpl implements SmsAcceptedService {

    private static final String SMS_ACCEPTED_CONFIG_TYPE = "NOTIFICATION_SMS_ACCEPTED";

    private final SmsAcceptedRepository smsAcceptedRepository;
    private final MobileBankingConfig mobileBankingConfig;
    private final HttpClientUtil httpClientUtil;
    private final OracleSmsHelper oracleSmsHelper;
    private final NotificationConfigRepository notificationConfigRepository;

    @Override
    @Transactional
    public SendBatchSmsResponse processSms() {
        log.info("Starting batch SMS processing from Oracle D_CBS_SMS_LOG");

        NotificationConfig smsConfig = notificationConfigRepository.findActiveByConfigType(SMS_ACCEPTED_CONFIG_TYPE)
                .orElseThrow(() -> new RuntimeException("SMS Accepted configuration not found"));

        String smsMessage = smsConfig.getConfigValue();
        log.info("Using SMS template: {}", smsConfig.getDescription());

        List<OracleSmsDto> processingRecords = oracleSmsHelper.selectProcessingSmsRecords();
        log.info("Found {} PROCESSING SMS records from Oracle", processingRecords.size());

        int successCount = 0;
        int failureCount = 0;

        for (OracleSmsDto record : processingRecords) {
            try {
                boolean success = sendSoapSms(record.getPhone(), smsMessage, record.getMsgId());
                if (success) {
                    oracleSmsHelper.updateSmsStatus(record.getMsgId(), "SUCCESS");
                    logSmsToPostgres(record.getMsgId(), record.getPhone(), smsMessage, "SUCCESS", null);
                    successCount++;
                } else {
                    oracleSmsHelper.updateSmsStatus(record.getMsgId(), "FAILURE");
                    logSmsToPostgres(record.getMsgId(), record.getPhone(), smsMessage, "FAILURE", "SOAP response error");
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Error sending SMS - msgId: {}, phone: {}", record.getMsgId(), record.getPhone(), e);
                oracleSmsHelper.updateSmsStatus(record.getMsgId(), "FAILURE");
                logSmsToPostgres(record.getMsgId(), record.getPhone(), smsMessage, "FAILURE", e.getMessage());
                failureCount++;
            }
        }

        int totalProcessed = processingRecords.size();
        log.info("Batch SMS processing completed - Total: {}, Success: {}, Failure: {}", totalProcessed, successCount, failureCount);

        return SendBatchSmsResponse.builder()
                .totalProcessed(totalProcessed)
                .successCount(successCount)
                .failureCount(failureCount)
                .message("SMS batch processing completed")
                .build();
    }

    private boolean sendSoapSms(String phone, String message, String msgId) {
        String otpUrl = mobileBankingConfig.getOtpUrl();
        String secretKey = mobileBankingConfig.getSecretKey();

        String soapXml = buildSoapRequest(msgId, phone, message, secretKey);

        try {
            log.info("Sending SOAP SMS - msgId: {}, phone: {}", msgId, phone);

            String responseXml = httpClientUtil.postForString(otpUrl, soapXml, "application/soap+xml");

            Matcher matcher = Pattern.compile("<(?:\\w+:)?return>(.*?)</(?:\\w+:)?return>").matcher(responseXml);
            String jsonPayload = matcher.find() ? matcher.group(1) : null;

            if (jsonPayload != null && jsonPayload.contains("\"rescode\":\"00\"")) {
                log.info("SMS sent successfully - msgId: {}, phone: {}", msgId, phone);
                return true;
            } else {
                log.warn("SMS sending failed - msgId: {}, response: {}", msgId, jsonPayload);
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending SOAP SMS - msgId: {}, phone: {}", msgId, phone, e);
            return false;
        }
    }

    private String buildSoapRequest(String requestID, String phone, String message, String secretKey) {
        return "<?xml version=\"1.0\"?>"
                + "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' "
                + "xmlns:cpb='http://cpbmobile.vnpay.vn'>"
                + "<soap:Header/>"
                + "<soap:Body>"
                + "<cpb:sendSmsNew>"
                + "<cpb:requestId>" + requestID + "</cpb:requestId>"
                + "<cpb:keyword>CPBSMS</cpb:keyword>"
                + "<cpb:mobileNo>" + phone + "</cpb:mobileNo>"
                + "<cpb:content>" + message + "</cpb:content>"
                + "<cpb:requestTime></cpb:requestTime>"
                + "<cpb:contentType>9</cpb:contentType>"
                + "<cpb:secretKey>" + secretKey + "</cpb:secretKey>"
                + "</cpb:sendSmsNew>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    private void logSmsToPostgres(String msgId, String phone, String message, String status, String errorDetails) {
        SmsAcceptedLog smsLog = SmsAcceptedLog.builder()
                .msgId(msgId)
                .phone(phone)
                .smsContent(message)
                .smsStatus(status)
                .errorDetails(errorDetails)
                .createdAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .build();

        smsAcceptedRepository.save(smsLog);
        log.debug("Logged SMS to PostgreSQL - msgId: {}, status: {}", msgId, status);
    }
}
