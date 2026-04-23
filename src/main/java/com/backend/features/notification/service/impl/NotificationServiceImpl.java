package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
import com.backend.features.notification.dto.LoanLateReminderDto;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.dto.SendSmsRequestDto;
import com.backend.features.notification.enums.ProcessingResult;
import com.backend.features.notification.helper.CpbHelper;
import com.backend.features.notification.helper.NotificationPayloadBuilder;
import com.backend.features.notification.helper.OracleHelper;
import com.backend.features.notification.models.ProcessingStatus;
import com.backend.features.notification.models.SmsFailureLog;
import com.backend.features.notification.models.SmsLog;
import com.backend.features.notification.repository.ProcessingStatusRepository;
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

    private final RestTemplate restTemplate;
    private final NotificationPayloadBuilder payloadBuilder;
    private final CpbApiConfig cpbApiConfig;
    private final OracleHelper oracleHelper;
    private final SmsLogRepository smsLogRepository;
    private final SmsFailureLogRepository smsFailureLogRepository;
    private final ProcessingStatusRepository processingStatusRepository;
    private final CpbHelper cpbHelper;

    @Override
    public ProcessingResult processPendingSmsNotifications() {
        log.info("START: Processing SMS notifications");

        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);

            log.info("Checking ProcessingStatus for date: {}", reportDate);
            Optional<ProcessingStatus> existingStatus = processingStatusRepository.findCompleteByReportDate(reportDate);

            if (existingStatus.isPresent()) {
                ProcessingStatus status = existingStatus.get();
                log.info("OPTIMIZATION HIT: Processing already complete for {}", reportDate);
                log.info("Details - Total: {} | Success: {} | Failure: {} | Completed: {}",
                        status.getTotalCustomers(), status.getSuccessCount(), status.getFailureCount(), status.getCompletedAt());
                log.info("Skipping View & SmsLog queries - returning ALL_SUCCESS");
                return ProcessingResult.ALL_SUCCESS;
            }

            log.info("No optimization hit, proceeding with full processing");
            log.info("Loading message content from CPB");
            String messageContent = cpbHelper.getContentDescription();

            log.info("Querying View: SELECT * FROM VIEW_LOAN_LATE_REMINDER");
            List<LoanLateReminderDto> records = oracleHelper.selectLoanLateReminderRecords();

            if (records.isEmpty()) {
                log.info("View is empty - COB not yet finished");
                log.info("END: Processing SMS notifications");
                return ProcessingResult.COB_NOT_FINISHED;
            }

            log.info("COB finished - Found {} records to process", records.size());

            int successCount = 0;
            int failureCount = 0;

            for (LoanLateReminderDto record : records) {
                try {
                    Optional<SmsLog> existingLog = smsLogRepository.findByCustomerIdAndPhoneNumberAndReportDateAndStatus(
                            record.getCustomerId(), record.getPhoneNumber(), record.getReportDate(), SMS_STATUS_SUCCESS);

                    if (existingLog.isPresent()) {
                        continue;
                    }

                    log.info("Sending SMS to phone: {} | Customer: {}", record.getPhoneNumber(), record.getCustomerId());

                    sendSmsToApi(record.getPhoneNumber(), messageContent);
                    successCount++;

                    logToPostgresSQL(record, SMS_STATUS_SUCCESS, messageContent);

                } catch (Exception e) {
                    failureCount++;
                    log.error("SMS sending failed for phone: {} | Error: {}", record.getPhoneNumber(), e.getMessage());

                    logToPostgresSQL(record, SMS_STATUS_FAILURE, messageContent);
                    recordFailureLog(record, e.getMessage());
                }
            }

            log.info("Processing result - Total: {} | Success: {} | Failure: {}", records.size(), successCount, failureCount);

            if (failureCount == 0) {
                log.info("All SMS sent successfully - creating ProcessingStatus record");
                createProcessingStatus(reportDate, records.size(), successCount, failureCount);
                log.info("END: Processing SMS notifications");
                return ProcessingResult.ALL_SUCCESS;
            } else {
                log.info("Found {} failures - will retry next hour", failureCount);
                log.info("END: Processing SMS notifications");
                return ProcessingResult.WITH_FAILURES;
            }

        } catch (Exception e) {
            log.error("ERROR in processPendingSmsNotifications: {}", e.getMessage(), e);
            log.info("END: Processing SMS notifications");
            return ProcessingResult.WITH_FAILURES;
        }
    }

    private void createProcessingStatus(LocalDate reportDate, int totalCustomers, int successCount, int failureCount) {
        try {
            ProcessingStatus status = ProcessingStatus.builder()
                    .reportDate(reportDate)
                    .totalCustomers(totalCustomers)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .isComplete(true)
                    .completedAt(LocalDateTime.now())
                    .lastCheckedAt(LocalDateTime.now())
                    .notes("All SMS processed successfully")
                    .build();

            processingStatusRepository.save(status);
            log.info("ProcessingStatus created for {}: complete=true", reportDate);

        } catch (Exception e) {
            log.error("ERROR creating ProcessingStatus: {}", e.getMessage(), e);
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

        } catch (Exception e) {
            log.error("ERROR recording failure log for phone: {}: {}", record.getPhoneNumber(), e.getMessage(), e);
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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getBody() != null && apiResponse.getBody().getDesc() != null) {
                return apiResponse.getBody().getDesc();
            }

            log.warn("No response body received from CPB API");
            return SMS_STATUS_FAILURE;

        } catch (RestClientException e) {
            log.error("CPB API request failed for phone: {}: {}", phoneNumber, e.getMessage(), e);
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

        } catch (Exception e) {
            log.error("ERROR saving audit log for phone: {}: {}", record.getPhoneNumber(), e.getMessage(), e);
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

        } catch (Exception e) {
            log.error("ERROR saving SMS audit log for phone: {}: {}", phoneNumber, e.getMessage(), e);
        }
    }

    @Override
    public String sendTestSms(SendSmsRequestDto request) {
        log.info("TEST SMS: Sending to phone: {}", request.getPhoneNumber());

        try {
            sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());
            logToPostgreSQLSimple(request.getPhoneNumber(), SMS_STATUS_SUCCESS, request.getMessageContent());

            log.info("TEST SMS: Sent successfully");
            return SMS_STATUS_SUCCESS;

        } catch (Exception e) {
            log.error("TEST SMS: Failed for phone: {}: {}", request.getPhoneNumber(), e.getMessage(), e);
            logToPostgreSQLSimple(request.getPhoneNumber(), SMS_STATUS_FAILURE, request.getMessageContent());
            throw new RuntimeException("Failed to send test SMS: " + e.getMessage(), e);
        }
    }

    // ============ SCHEDULER BUSINESS LOGIC ============

    public void processWithRetry(String timeLabel) {
        log.info("START: Hourly SMS processing at {}", timeLabel);

        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);
            log.info("Processing for report date: {}", reportDate);

            ProcessingResult result = processPendingSmsNotifications();

            if (result == ProcessingResult.COB_NOT_FINISHED) {
                log.info("RESULT: COB not finished - skipping processing");
                return;
            }

            if (isProcessingComplete(reportDate)) {
                log.info("RESULT: All SMS processing complete - no more failures");
                return;
            }

            log.info("RESULT: {} - will retry failures next hour", result.getDescription());

        } catch (Exception e) {
            log.error("ERROR in processWithRetry at {}: {}", timeLabel, e.getMessage(), e);
        }

        log.info("END: Hourly SMS processing");
    }

    private boolean isProcessingComplete(LocalDate reportDate) {
        try {
            log.info("Checking completion status for {}", reportDate);
            List<SmsFailureLog> failedRecords = smsFailureLogRepository.findFailedRecordsByReportDate(reportDate);

            if (!failedRecords.isEmpty()) {
                log.info("Processing NOT complete: {} failures still exist", failedRecords.size());
                return false;
            }

            log.info("Processing complete: No failures remaining");
            return true;

        } catch (Exception e) {
            log.error("ERROR checking completion status: {}", e.getMessage(), e);
            return false;
        }
    }

}
