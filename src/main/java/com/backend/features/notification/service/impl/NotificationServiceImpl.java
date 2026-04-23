package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.dto.SendSmsRequestDto;
import com.backend.features.notification.helper.CpbHelper;
import com.backend.features.notification.helper.NotificationPayloadBuilder;
import com.backend.features.notification.helper.OracleHelper;
import com.backend.features.notification.models.SmsLog;
import com.backend.features.notification.repository.SmsLogRepository;
import com.backend.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String SMS_STATUS_SUCCESS = "SVC-SUCCESS-00";
    private static final String SMS_STATUS_FAILED = "SVC-FAILED";

    private final RestTemplate restTemplate;
    private final NotificationPayloadBuilder payloadBuilder;
    private final CpbApiConfig cpbApiConfig;
    private final OracleHelper oracleHelper;
    private final SmsLogRepository smsLogRepository;
    private final CpbHelper cpbHelper;

    @Override
    public void processPendingSmsNotifications() {
        log.info("========== START: Processing pending SMS notifications from Oracle ==========");

        try {
            String messageContent = cpbHelper.getContentDescription();
            log.info("SMS message content loaded from D_CBS_SETTING: {}", messageContent);

            List<String> phoneNumbers = oracleHelper.selectPendingSmsPhoneNumbers();

            if (phoneNumbers.isEmpty()) {
                log.info("✓ No pending SMS notifications found");
                log.info("========== END: Processing pending SMS notifications ==========");
                return;
            }

            log.info("✓ Found {} pending SMS notifications to process", phoneNumbers.size());

            int successCount = 0;
            int failureCount = 0;

            for (String phoneNumber : phoneNumbers) {
                try {
                    log.info("========== START: Processing SMS for phone: {} ==========", phoneNumber);

                    sendSmsToApi(phoneNumber, messageContent);
                    oracleHelper.updateSmsStatus(phoneNumber, SMS_STATUS_SUCCESS);
                    successCount++;

                    logToPostgresSQL(phoneNumber, SMS_STATUS_SUCCESS, messageContent);
                    log.info("✓ SMS sent successfully | Phone: {} | Status: {}", phoneNumber, SMS_STATUS_SUCCESS);
                    log.info("========== END: SMS processing completed for phone: {} ==========", phoneNumber);

                } catch (Exception e) {
                    failureCount++;
                    log.error("✗ SMS sending failed | Phone: {} | Error: {}", phoneNumber, e.getMessage());
                    logToPostgresSQL(phoneNumber, SMS_STATUS_FAILED, messageContent);
                    try {
                        oracleHelper.updateSmsStatus(phoneNumber, SMS_STATUS_FAILED);
                    } catch (Exception ex) {
                        log.error("✗ Failed to update Oracle status for phone: {} | Error: {}", phoneNumber, ex.getMessage());
                    }
                }
            }

            log.info("========== RESULT: Success: {}, Failed: {} ==========", successCount, failureCount);
            log.info("========== END: Processing pending SMS notifications ==========");

        } catch (Exception e) {
            log.error("✗ Exception in Pending SMS Processing: {} | Cause: {}", e.getMessage(), e.getCause(), e);
        }
    }

    private String sendSmsToApi(String phoneNumber, String messageContent) throws RestClientException {
        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";
            log.info("API: Sending SMS to {}", apiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getBody() != null && apiResponse.getBody().getDesc() != null) {
                String responseStatus = apiResponse.getBody().getDesc();
                log.info("API: Response status received: {}", responseStatus);
                return responseStatus;
            }

            log.warn("API: No response body received");
            return SMS_STATUS_FAILED;

        } catch (RestClientException e) {
            log.error("API: Request failed for phone: {}", phoneNumber, e);
            throw e;
        }
    }

    private void logToPostgresSQL(String phoneNumber, String status, String messageContent) {
        try {
            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(phoneNumber)
                    .messageContent(messageContent)
                    .smsStatus(status)
                    .smsLogDate(LocalDateTime.now())
                    .build();

            smsLogRepository.save(smsLog);
            log.info("PostgreSQL: Audit log saved for phone: {} | Status: {}", phoneNumber, status);

        } catch (Exception e) {
            log.error("PostgreSQL: Failed to save audit log for phone: {} | Error: {}", phoneNumber, e.getMessage());
        }
    }

    @Override
    public String sendTestSms(SendSmsRequestDto request) {
        log.info("========== START: Sending test SMS ==========");
        try {
            sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());
            logToPostgresSQL(request.getPhoneNumber(), SMS_STATUS_SUCCESS, request.getMessageContent());
            log.info("✓ Test SMS sent successfully | Phone: {} | Status: {}", request.getPhoneNumber(), SMS_STATUS_SUCCESS);
            log.info("========== END: Test SMS sent ==========");
            return SMS_STATUS_SUCCESS;
        } catch (Exception e) {
            log.error("✗ Test SMS sending failed | Phone: {} | Error: {}", request.getPhoneNumber(), e.getMessage());
            logToPostgresSQL(request.getPhoneNumber(), SMS_STATUS_FAILED, request.getMessageContent());
            throw new RuntimeException("Failed to send test SMS: " + e.getMessage(), e);
        }
    }
}
