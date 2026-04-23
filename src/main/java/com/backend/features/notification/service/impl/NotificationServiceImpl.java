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

    private static final String SMS_STATUS_SUCCESS = "SUCCESS";
    private static final String SMS_STATUS_FAILURE = "FAILURE";
    private static final int PROCESSING_COMPLETE_THRESHOLD_MINUTES = 30;

    private final RestTemplate restTemplate;
    private final NotificationPayloadBuilder payloadBuilder;
    private final CpbApiConfig cpbApiConfig;
    private final OracleHelper oracleHelper;
    private final SmsLogRepository smsLogRepository;
    private final SmsFailureLogRepository smsFailureLogRepository;
    private final CpbHelper cpbHelper;

    // Track when processing last happened (for this day)
    private LocalDateTime lastProcessingTime = null;

    @Override
    public void processPendingSmsNotifications() {
        log.info("========== START: Processing SMS notifications ==========");

        try {
            String messageContent = cpbHelper.getContentDescription();
            log.info("SMS message content loaded: {}", messageContent);

            List<LoanLateReminderDto> records = oracleHelper.selectLoanLateReminderRecords();

            if (records.isEmpty()) {
                log.info("✓ No loan late reminder records found (view may be empty if COB not complete)");
                log.info("========== END: Processing SMS notifications ==========");
                return;
            }

            log.info("✓ Found {} loan late reminder records to process", records.size());

            int successCount = 0;
            int failureCount = 0;

            for (LoanLateReminderDto record : records) {
                try {
                    Optional<SmsLog> existingLog = smsLogRepository.findByCustomerIdAndPhoneNumberAndReportDateAndStatus(
                            record.getCustomerId(), record.getPhoneNumber(), record.getReportDate(), SMS_STATUS_SUCCESS);
                    if (existingLog.isPresent()) {
                        log.info("⊘ SMS already sent successfully | Customer: {} | Phone: {} | Date: {} | Skipping",
                                record.getCustomerId(), record.getPhoneNumber(), record.getReportDate());
                        continue;
                    }

                    log.info("========== START: Processing SMS for phone: {} | Customer: {} ==========",
                            record.getPhoneNumber(), record.getCustomerId());

                    sendSmsToApi(record.getPhoneNumber(), messageContent);
                    successCount++;

                    logToPostgresSQL(record, SMS_STATUS_SUCCESS, messageContent);
                    log.info("✓ SMS sent successfully | Phone: {} | Status: {}", record.getPhoneNumber(), SMS_STATUS_SUCCESS);
                    log.info("========== END: SMS processing completed for phone: {} ==========", record.getPhoneNumber());

                } catch (Exception e) {
                    failureCount++;
                    log.error("✗ SMS sending failed | Phone: {} | Customer: {} | Error: {}",
                            record.getPhoneNumber(), record.getCustomerId(), e.getMessage());
                    logToPostgresSQL(record, SMS_STATUS_FAILURE, messageContent);
                    recordFailureLog(record, e.getMessage());
                }
            }

            log.info("========== RESULT: Success: {}, Failed: {} ==========", successCount, failureCount);
            log.info("========== END: Processing SMS notifications ==========");

        } catch (Exception e) {
            log.error("✗ Exception in SMS Processing: {} | Cause: {}", e.getMessage(), e.getCause(), e);
        }
    }

    public String getMessageContent() {
        return cpbHelper.getContentDescription();
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
                            .build());

            failureLog.setFailureReason(failureReason);
            failureLog.setRetryCount(failureLog.getRetryCount() + 1);
            failureLog.setLastRetryDate(LocalDateTime.now());
            failureLog.setStatus(SMS_STATUS_FAILURE);

            smsFailureLogRepository.save(failureLog);
            log.info("Failure logged for phone: {} | Retry count: {}", record.getPhoneNumber(), failureLog.getRetryCount());

        } catch (Exception e) {
            log.error("Failed to record failure log for phone: {}", record.getPhoneNumber(), e);
        }
    }

    public void sendSmsDirectly(String phoneNumber, String messageContent) throws RestClientException {
        sendSmsToApi(phoneNumber, messageContent);
        logToPostgreSQLSimple(phoneNumber, SMS_STATUS_SUCCESS, messageContent);
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
            return SMS_STATUS_FAILURE;

        } catch (RestClientException e) {
            log.error("API: Request failed for phone: {}", phoneNumber, e);
            throw e;
        }
    }

    private void logToPostgresSQL(LoanLateReminderDto record, String status, String messageContent) {
        try {
            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(record.getPhoneNumber())
                    .customerId(record.getCustomerId())
                    .reportDate(record.getReportDate())
                    .arrangementId(record.getArrangementId())
                    .dayDue(record.getDayDue())
                    .messageContent(messageContent)
                    .smsStatus(status)
                    .smsLogDate(LocalDateTime.now())
                    .build();

            smsLogRepository.save(smsLog);
            log.info("PostgreSQL: Audit log saved for phone: {} | Customer: {} | Status: {}",
                    record.getPhoneNumber(), record.getCustomerId(), status);

        } catch (Exception e) {
            log.error("PostgreSQL: Failed to save audit log for phone: {} | Error: {}",
                    record.getPhoneNumber(), e.getMessage());
        }
    }

    private void logToPostgreSQLSimple(String phoneNumber, String status, String messageContent) {
        try {
            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(phoneNumber)
                    .messageContent(messageContent)
                    .smsStatus(status)
                    .smsLogDate(LocalDateTime.now())
                    .build();

            smsLogRepository.save(smsLog);
            log.info("PostgreSQL: Test SMS audit log saved for phone: {} | Status: {}", phoneNumber, status);

        } catch (Exception e) {
            log.error("PostgreSQL: Failed to save test SMS audit log for phone: {} | Error: {}", phoneNumber, e.getMessage());
        }
    }

    @Override
    public String sendTestSms(SendSmsRequestDto request) {
        log.info("========== START: Sending test SMS ==========");
        try {
            sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());
            logToPostgreSQLSimple(request.getPhoneNumber(), SMS_STATUS_SUCCESS, request.getMessageContent());
            log.info("✓ Test SMS sent successfully | Phone: {} | Status: {}", request.getPhoneNumber(), SMS_STATUS_SUCCESS);
            log.info("========== END: Test SMS sent ==========");
            return SMS_STATUS_SUCCESS;
        } catch (Exception e) {
            log.error("✗ Test SMS sending failed | Phone: {} | Error: {}", request.getPhoneNumber(), e.getMessage());
            logToPostgreSQLSimple(request.getPhoneNumber(), SMS_STATUS_FAILURE, request.getMessageContent());
            throw new RuntimeException("Failed to send test SMS: " + e.getMessage(), e);
        }
    }

    // ============ SCHEDULER BUSINESS LOGIC ============

    public void processWithRetry(String timeLabel) {
        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);

            // STEP 1: Check if processing is already complete
            if (isProcessingComplete(reportDate)) {
                log.info("✓ Processing already complete at {}", timeLabel);
                log.info("  All SMS sent successfully - no failures remaining");
                log.info("  Skipping processing to save resources");
                return;
            }

            // STEP 2: Process new records from view (if COB finished)
            log.info("STEP 1: Processing new records from view ({})", timeLabel);
            processPendingSmsNotifications();

            // Update tracking after processing
            lastProcessingTime = LocalDateTime.now();

            // STEP 3: Retry failed records
            log.info("STEP 2: Retrying failed records ({})", timeLabel);
            retryFailedRecords(reportDate);

            // STEP 4: Check completion status after retry
            if (isProcessingComplete(reportDate)) {
                log.info("✓ All SMS processing complete at {}", timeLabel);
            }

        } catch (Exception e) {
            log.error("✗ Exception in SMS Processing at {}: {} | Cause: {}",
                    timeLabel, e.getMessage(), e.getCause(), e);
        }
    }

    private boolean isProcessingComplete(LocalDate reportDate) {
        try {
            // Check 1: No failures in SmsFailureLog
            List<SmsFailureLog> failedRecords = smsFailureLogRepository.findFailedRecordsByReportDate(reportDate);

            if (!failedRecords.isEmpty()) {
                log.debug("Processing NOT complete: {} failures still exist", failedRecords.size());
                return false;
            }

            // Check 2: We must have processed at least once (lastProcessingTime set)
            if (lastProcessingTime == null) {
                log.debug("Processing NOT complete: No records processed yet (COB might not be finished)");
                return false;
            }

            // Check 3: Enough time has passed since last processing
            LocalDateTime thresholdTime = lastProcessingTime.plusMinutes(PROCESSING_COMPLETE_THRESHOLD_MINUTES);
            if (LocalDateTime.now().isBefore(thresholdTime)) {
                log.debug("Processing NOT complete: Not enough time passed since last processing");
                return false;
            }

            log.info("✓ Processing COMPLETE: Zero failures + time threshold passed");
            return true;

        } catch (Exception e) {
            log.error("✗ Error checking completion status: {}", e.getMessage(), e);
            return false;
        }
    }

    private void retryFailedRecords(LocalDate reportDate) {
        try {
            String messageContent = null;

            try {
                messageContent = getMessageContent();
            } catch (Exception e) {
                log.warn("Could not load message content: {}", e.getMessage());
                messageContent = "";
            }

            List<SmsFailureLog> failedRecords = smsFailureLogRepository.findFailedRecordsByReportDate(reportDate);

            if (failedRecords.isEmpty()) {
                log.info("✓ No failed records to retry for date: {}", reportDate);
                return;
            }

            log.info("✓ Found {} failed SMS records to retry for date: {}", failedRecords.size(), reportDate);

            int retrySuccessCount = 0;
            int retryFailureCount = 0;

            for (SmsFailureLog failureLog : failedRecords) {
                try {
                    log.info("  Retrying SMS for phone: {} | Retry count: {} | Reason: {}",
                            failureLog.getPhoneNumber(),
                            failureLog.getRetryCount(),
                            failureLog.getFailureReason());

                    sendSmsDirectly(failureLog.getPhoneNumber(), messageContent);
                    retrySuccessCount++;

                    failureLog.setStatus(SMS_STATUS_SUCCESS);
                    failureLog.setLastRetryDate(LocalDateTime.now());
                    smsFailureLogRepository.save(failureLog);

                    log.info("  ✓ Retry successful for phone: {}", failureLog.getPhoneNumber());

                } catch (Exception e) {
                    retryFailureCount++;
                    log.error("  ✗ Retry failed for phone: {} | Error: {}",
                            failureLog.getPhoneNumber(), e.getMessage());

                    failureLog.setRetryCount(failureLog.getRetryCount() + 1);
                    failureLog.setFailureReason(e.getMessage());
                    failureLog.setLastRetryDate(LocalDateTime.now());
                    failureLog.setStatus(SMS_STATUS_FAILURE);
                    smsFailureLogRepository.save(failureLog);
                }
            }

            log.info("========== RETRY SUMMARY: Success: {}, Failed: {} ==========",
                    retrySuccessCount, retryFailureCount);

        } catch (Exception e) {
            log.error("✗ Exception in SMS Retry Processing: {} | Cause: {}",
                    e.getMessage(), e.getCause(), e);
        }
    }
}
