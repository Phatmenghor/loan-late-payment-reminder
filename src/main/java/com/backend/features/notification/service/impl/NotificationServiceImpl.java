package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
import com.backend.features.notification.dto.LoanLateReminderDto;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.dto.SendSmsRequestDto;
import com.backend.features.notification.helper.CpbHelper;
import com.backend.features.notification.helper.NotificationPayloadBuilder;
import com.backend.features.notification.helper.OracleHelper;
import com.backend.features.notification.models.SmsFailureLog;
import com.backend.features.notification.models.SmsLog;
import com.backend.features.notification.repository.SmsFailureLogRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final SmsFailureLogRepository smsFailureLogRepository;
    private final CpbHelper cpbHelper;

    @Override
    public void processPendingSmsNotifications() {
        log.info("========== START: Processing SMS notifications from STG.VIEW_LOAN_LATE_REMINDER ==========");

        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);
            String messageContent = cpbHelper.getContentDescription();
            log.info("SMS message content loaded: {}", messageContent);

            List<LoanLateReminderDto> records = oracleHelper.selectLoanLateReminderRecords(reportDate);

            if (records.isEmpty()) {
                log.info("✓ No loan late reminder records found for date: {}", reportDate);
                log.info("========== END: Processing SMS notifications ==========");
                return;
            }

            log.info("✓ Found {} loan late reminder records to process for date: {}", records.size(), reportDate);

            int successCount = 0;
            int failureCount = 0;

            for (LoanLateReminderDto record : records) {
                try {
                    Optional<SmsLog> existingLog = smsLogRepository.findByPhoneNumberAndStatus(record.getPhoneNumber(), SMS_STATUS_SUCCESS);
                    if (existingLog.isPresent()) {
                        log.info("⊘ SMS already sent successfully | Phone: {} | Customer: {} | Skipping",
                                record.getPhoneNumber(), record.getCustomerId());
                        continue;
                    }

                    log.info("========== START: Processing SMS for phone: {} | Customer: {} ==========",
                            record.getPhoneNumber(), record.getCustomerId());

                    sendSmsToApi(record.getPhoneNumber(), messageContent);
                    successCount++;

                    logToPostgresSQL(record.getPhoneNumber(), SMS_STATUS_SUCCESS, messageContent);
                    log.info("✓ SMS sent successfully | Phone: {} | Status: {}", record.getPhoneNumber(), SMS_STATUS_SUCCESS);
                    log.info("========== END: SMS processing completed for phone: {} ==========", record.getPhoneNumber());

                } catch (Exception e) {
                    failureCount++;
                    log.error("✗ SMS sending failed | Phone: {} | Customer: {} | Error: {}",
                            record.getPhoneNumber(), record.getCustomerId(), e.getMessage());
                    logToPostgresSQL(record.getPhoneNumber(), SMS_STATUS_FAILED, messageContent);
                    recordFailureLog(record, e.getMessage());
                }
            }

            log.info("========== RESULT: Success: {}, Failed: {} ==========", successCount, failureCount);
            log.info("========== END: Processing SMS notifications ==========");

        } catch (Exception e) {
            log.error("✗ Exception in SMS Processing: {} | Cause: {}", e.getMessage(), e.getCause(), e);
        }
    }

    private void recordFailureLog(LoanLateReminderDto record, String failureReason) {
        try {
            LocalDate reportDate = record.getReportDate() != null ? record.getReportDate() : LocalDate.now().minusDays(1);

            Optional<SmsFailureLog> existingRecord = smsFailureLogRepository
                    .findByPhoneAndDate(record.getPhoneNumber(), reportDate);

            SmsFailureLog failureLog = existingRecord.orElseGet(() ->
                    SmsFailureLog.builder()
                            .phoneNumber(record.getPhoneNumber())
                            .customerId(record.getCustomerId())
                            .reportDate(reportDate)
                            .arrangementId(record.getArrangementId())
                            .retryCount(0)
                            .createdDate(LocalDateTime.now())
                            .build());

            failureLog.setFailureReason(failureReason);
            failureLog.setRetryCount(failureLog.getRetryCount() + 1);
            failureLog.setLastRetryDate(LocalDateTime.now());
            failureLog.setStatus(SMS_STATUS_FAILED);
            failureLog.setUpdatedDate(LocalDateTime.now());

            smsFailureLogRepository.save(failureLog);
            log.info("Failure logged for phone: {} | Retry count: {}", record.getPhoneNumber(), failureLog.getRetryCount());

        } catch (Exception e) {
            log.error("Failed to record failure log for phone: {}", record.getPhoneNumber(), e);
        }
    }

    public void sendSmsDirectly(String phoneNumber, String messageContent) throws RestClientException {
        sendSmsToApi(phoneNumber, messageContent);
        logToPostgresSQL(phoneNumber, SMS_STATUS_SUCCESS, messageContent);
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
