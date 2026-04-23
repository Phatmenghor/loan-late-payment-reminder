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
        log.info("┌─────────────────────────────────────────────────────────────────────┐");
        log.info("│ STEP 1: Processing SMS Notifications - Check View for COB Data     │");
        log.info("└─────────────────────────────────────────────────────────────────────┘");

        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);

            log.info("   ├─ Optimization Check: Query ProcessingStatus for {}", reportDate);
            Optional<ProcessingStatus> existingStatus = processingStatusRepository.findCompleteByReportDate(reportDate);

            if (existingStatus.isPresent()) {
                ProcessingStatus status = existingStatus.get();
                log.info("   ├─ ✓ OPTIMIZATION HIT! Processing already complete");
                log.info("   ├─ Details:");
                log.info("   │  ├─ Report Date: {}", status.getReportDate());
                log.info("   │  ├─ Total Customers: {}", status.getTotalCustomers());
                log.info("   │  ├─ Success Count: {}", status.getSuccessCount());
                log.info("   │  ├─ Failure Count: {}", status.getFailureCount());
                log.info("   │  ├─ Completed At: {}", status.getCompletedAt());
                log.info("   │  └─ Query Count: 1 (optimization saved {} queries!)",
                        status.getTotalCustomers() + 1);
                log.info("   │");
                log.info("   └─ Skipping View query & SmsLog checks → Returning: ALL_SUCCESS");
                return ProcessingResult.ALL_SUCCESS;
            }

            log.info("   │");
            log.info("   ├─ No optimization hit, proceeding with full processing");
            log.info("   ├─ Loading message content from CPB...");
            String messageContent = cpbHelper.getContentDescription();
            log.info("   │  └─ ✓ Message content loaded");

            log.info("   ├─ Querying View: SELECT * FROM VIEW_LOAN_LATE_REMINDER");
            List<LoanLateReminderDto> records = oracleHelper.selectLoanLateReminderRecords();
            log.info("   │  └─ ✓ View query executed");

            if (records.isEmpty()) {
                log.warn("   ├─ ⊘ View is EMPTY - COB not yet finished");
                log.info("   │  └─ No records to process");
                log.info("   └─ Returning: COB_NOT_FINISHED");
                return ProcessingResult.COB_NOT_FINISHED;
            }

            log.info("   ├─ ✓ COB FINISHED! View has {} records", records.size());
            log.info("   ├─ Processing each record:");

            int successCount = 0;
            int failureCount = 0;

            for (LoanLateReminderDto record : records) {
                try {
                    Optional<SmsLog> existingLog = smsLogRepository.findByCustomerIdAndPhoneNumberAndReportDateAndStatus(
                            record.getCustomerId(), record.getPhoneNumber(), record.getReportDate(), SMS_STATUS_SUCCESS);

                    if (existingLog.isPresent()) {
                        log.debug("   │  ├─ [SKIP] Phone: {} | Already sent", record.getPhoneNumber());
                        continue;
                    }

                    log.info("   │  ├─ [SEND] Phone: {} | Customer: {}",
                            record.getPhoneNumber(), record.getCustomerId());

                    sendSmsToApi(record.getPhoneNumber(), messageContent);
                    successCount++;

                    logToPostgresSQL(record, SMS_STATUS_SUCCESS, messageContent);
                    log.info("   │  │  └─ ✓ SUCCESS");

                } catch (Exception e) {
                    failureCount++;
                    log.error("   │  ├─ [FAIL] Phone: {} | Error: {}",
                            record.getPhoneNumber(), e.getMessage());

                    logToPostgresSQL(record, SMS_STATUS_FAILURE, messageContent);
                    recordFailureLog(record, e.getMessage());
                }
            }

            log.info("   │");
            log.info("   ├─ Processing Complete:");
            log.info("   │  ├─ Total: {}", records.size());
            log.info("   │  ├─ Success: {}", successCount);
            log.info("   │  ├─ Failure: {}", failureCount);
            log.info("   │  └─ Queries: {} (View + SmsLog checks)", records.size() + 1);

            if (failureCount == 0) {
                log.info("   │");
                log.info("   ├─ ✓ ALL SUCCESS! Creating ProcessingStatus record...");
                createProcessingStatus(reportDate, records.size(), successCount, failureCount);
                log.info("   │  └─ ProcessingStatus saved (optimization for next run)");
                log.info("   └─ Returning: ALL_SUCCESS");
                return ProcessingResult.ALL_SUCCESS;
            } else {
                log.info("   └─ Returning: WITH_FAILURES ({} failures to retry next hour)", failureCount);
                return ProcessingResult.WITH_FAILURES;
            }

        } catch (Exception e) {
            log.error("   ✗ CRITICAL ERROR in processPendingSmsNotifications");
            log.error("   │ Error: {}", e.getMessage());
            log.error("   │ Cause: {}", e.getCause(), e);
            log.error("   └─ Returning: WITH_FAILURES");
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
                    .notes("All SMS processed successfully on " + LocalDateTime.now())
                    .build();

            processingStatusRepository.save(status);
            log.debug("   │  ProcessingStatus created: reportDate={}, isComplete=true", reportDate);

        } catch (Exception e) {
            log.error("   │  ✗ ERROR creating ProcessingStatus: {}", e.getMessage(), e);
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
            log.debug("   │ Failure Log Updated: Phone={} | RetryCount={} | Reason={}",
                    record.getPhoneNumber(), failureLog.getRetryCount(), failureReason);

        } catch (Exception e) {
            log.error("   │ ✗ ERROR recording failure log for phone: {} | Error: {}",
                    record.getPhoneNumber(), e.getMessage(), e);
        }
    }

    public void sendSmsDirectly(String phoneNumber, String messageContent) throws RestClientException {
        log.debug("   │  Sending SMS directly (retry attempt): {}", phoneNumber);
        sendSmsToApi(phoneNumber, messageContent);
        logToPostgreSQLSimple(phoneNumber, SMS_STATUS_SUCCESS, messageContent);
        log.debug("   │  Direct SMS sent and logged successfully");
    }

    private String sendSmsToApi(String phoneNumber, String messageContent) throws RestClientException {
        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";
            log.debug("   │  API URL: {}", apiUrl);
            log.debug("   │  Payload built for phone: {}", phoneNumber);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            log.debug("   │  Sending HTTP POST request to CPB API...");
            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getBody() != null && apiResponse.getBody().getDesc() != null) {
                String responseStatus = apiResponse.getBody().getDesc();
                log.debug("   │  CPB API Response Status: {}", responseStatus);
                return responseStatus;
            }

            log.warn("   │  ✗ No response body received from CPB API");
            return SMS_STATUS_FAILURE;

        } catch (RestClientException e) {
            log.error("   │  ✗ CPB API Request Failed for phone: {} | Error: {}",
                    phoneNumber, e.getMessage(), e);
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
            log.debug("   │ Audit Log Saved to SmsLog: Phone={} | Status={} | Date={}",
                    record.getPhoneNumber(), status, smsLog.getSmsLogDate());

        } catch (Exception e) {
            log.error("   │ ✗ ERROR saving audit log to SmsLog: Phone={} | Error: {}",
                    record.getPhoneNumber(), e.getMessage(), e);
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
            log.debug("   │ Test SMS Audit Log Saved: Phone={} | Status={}", phoneNumber, status);

        } catch (Exception e) {
            log.error("   │ ✗ ERROR saving test SMS audit log: Phone={} | Error: {}", phoneNumber, e.getMessage(), e);
        }
    }

    @Override
    public String sendTestSms(SendSmsRequestDto request) {
        log.info("┌─────────────────────────────────────────────────────────────────────┐");
        log.info("│ TEST SMS REQUEST                                                    │");
        log.info("├─────────────────────────────────────────────────────────────────────┤");
        log.info("│ Phone: {}", String.format("%-64s", request.getPhoneNumber() + " │"));
        log.info("│ Message: {}", String.format("%-62s", (request.getMessageContent().substring(0, Math.min(62, request.getMessageContent().length())) + "...") + " │"));
        log.info("└─────────────────────────────────────────────────────────────────────┘");

        try {
            log.info("   → Sending SMS via CPB API...");
            sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());
            logToPostgreSQLSimple(request.getPhoneNumber(), SMS_STATUS_SUCCESS, request.getMessageContent());

            log.info("   ✓ Test SMS sent successfully");
            log.info("   │ Status: {}", SMS_STATUS_SUCCESS);
            log.info("   └─ Audit log saved to SmsLog");
            return SMS_STATUS_SUCCESS;

        } catch (Exception e) {
            log.error("   ✗ Test SMS sending FAILED");
            log.error("   │ Phone: {}", request.getPhoneNumber());
            log.error("   │ Error: {}", e.getMessage());
            logToPostgreSQLSimple(request.getPhoneNumber(), SMS_STATUS_FAILURE, request.getMessageContent());
            log.error("   └─ Failure logged to SmsLog");
            throw new RuntimeException("Failed to send test SMS: " + e.getMessage(), e);
        }
    }

    // ============ SCHEDULER BUSINESS LOGIC ============

    public void processWithRetry(String timeLabel) {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════════════════╗");
        log.info("║ HOURLY SCHEDULED PROCESSING: {} (Processing Yesterday's Overdue Loans)  ║", String.format("%-38s", timeLabel));
        log.info("╚══════════════════════════════════════════════════════════════════════════╝");

        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);
            log.info("   Processing for Report Date: {}", reportDate);
            log.info("");

            ProcessingResult result = processPendingSmsNotifications();
            log.info("");

            if (result == ProcessingResult.COB_NOT_FINISHED) {
                log.info("┌─────────────────────────────────────────────────────────────────────┐");
                log.info("│ RESULT: COB NOT FINISHED - Skipping further processing              │");
                log.info("├─────────────────────────────────────────────────────────────────────┤");
                log.info("│ Action: RETURN early - wait for next hourly run                    │");
                log.info("│ Reason: View is empty, COB still populating data                   │");
                log.info("│ Next Step: Will retry at next scheduled hour                       │");
                log.info("└─────────────────────────────────────────────────────────────────────┘");
                return;
            }

            if (isProcessingComplete(reportDate)) {
                log.info("┌─────────────────────────────────────────────────────────────────────┐");
                log.info("│ RESULT: ALL SMS PROCESSING COMPLETE ✓✓✓                            │");
                log.info("├─────────────────────────────────────────────────────────────────────┤");
                log.info("│ Status: All SMS sent successfully                                   │");
                log.info("│ Failures: 0 (SmsFailureLog is empty)                               │");
                log.info("│ Action: Skipping future processing to save resources               │");
                log.info("│ Benefit: No unnecessary database queries/API calls                 │");
                log.info("└─────────────────────────────────────────────────────────────────────┘");
                return;
            }

            log.info("┌─────────────────────────────────────────────────────────────────────┐");
            log.info("│ PROCESSING STATUS: Continuing - Failures Still Exist                │");
            log.info("├─────────────────────────────────────────────────────────────────────┤");
            log.info("│ Status: {} - Retries will happen automatically                     │", result.getDescription());
            log.info("│ Action: processPendingSmsNotifications will retry failures on next│");
            log.info("│         hourly run by checking SmsLog for non-SUCCESS records    │");
            log.info("│ Next Step: Will retry at {}                                        │", getNextHourlyTime(timeLabel));
            log.info("└─────────────────────────────────────────────────────────────────────┘");

        } catch (Exception e) {
            log.error("╔══════════════════════════════════════════════════════════════════════════╗");
            log.error("║ ✗ CRITICAL ERROR in processWithRetry at {}                              ║", String.format("%-38s", timeLabel));
            log.error("╠══════════════════════════════════════════════════════════════════════════╣");
            log.error("║ Error Message: {}", String.format("%-59s", e.getMessage()));
            log.error("║ Exception Type: {}", String.format("%-56s", e.getClass().getSimpleName()));
            log.error("║ Root Cause: {}", String.format("%-60s", e.getCause() != null ? e.getCause().toString() : "N/A"));
            log.error("╚══════════════════════════════════════════════════════════════════════════╝");
            log.error("   Stack Trace:", e);
        }
    }

    private String getNextHourlyTime(String currentTime) {
        try {
            int currentHour = Integer.parseInt(currentTime.split(":")[0]);
            int nextHour = (currentHour + 1) % 24;
            return String.format("%02d:00", nextHour);
        } catch (Exception e) {
            return "Next Hour";
        }
    }

    private boolean isProcessingComplete(LocalDate reportDate) {
        log.info("┌─────────────────────────────────────────────────────────────────────┐");
        log.info("│ STEP 2: Checking Completion Status                                  │");
        log.info("└─────────────────────────────────────────────────────────────────────┘");

        try {
            log.info("   Querying SmsFailureLog for reportDate: {}", reportDate);
            List<SmsFailureLog> failedRecords = smsFailureLogRepository.findFailedRecordsByReportDate(reportDate);
            log.info("   Query result: {} failure records found", failedRecords.size());

            if (!failedRecords.isEmpty()) {
                log.info("   │");
                log.info("   ├─ Status: NOT COMPLETE");
                log.info("   ├─ Reason: {} failures still exist", failedRecords.size());
                log.info("   ├─ Details:");

                for (SmsFailureLog failure : failedRecords) {
                    log.info("   │  ├─ Phone: {} | Customer: {} | Retry Count: {} | Reason: {}",
                            failure.getPhoneNumber(), failure.getCustomerId(),
                            failure.getRetryCount(), failure.getFailureReason());
                }

                log.info("   │");
                log.info("   └─ Action: Will retry these records on next hourly run");
                return false;
            }

            log.info("   │");
            log.info("   ├─ Status: ✓✓✓ COMPLETE");
            log.info("   ├─ Reason: SmsFailureLog is EMPTY (no failures remaining)");
            log.info("   ├─ Verification: All customers have successful SMS");
            log.info("   │");
            log.info("   └─ Action: Stop processing, no future retries needed");
            return true;

        } catch (Exception e) {
            log.error("   ✗ ERROR checking completion status");
            log.error("   │ Error: {}", e.getMessage());
            log.error("   │ Exception: {}", e.getClass().getSimpleName());
            log.error("   │ Stack:", e);
            log.error("   └─ Action: Treat as NOT COMPLETE (safer approach)");
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
