package com.backend.features.notification.service;

import com.backend.features.notification.models.SmsFailureLog;
import com.backend.features.notification.repository.SmsFailureLogRepository;
import com.backend.features.notification.repository.SmsLogRepository;
import com.backend.features.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsRetrySchedulerService {

    private static final String SMS_STATUS_SUCCESS = "SUCCESS";
    private static final String SMS_STATUS_FAILURE = "FAILURE";
    private static final int PROCESSING_COMPLETE_THRESHOLD_MINUTES = 30;

    private final NotificationServiceImpl notificationService;
    private final SmsFailureLogRepository smsFailureLogRepository;
    private final SmsLogRepository smsLogRepository;

    // Track when processing last happened (for this day)
    private LocalDateTime lastProcessingTime = null;
    private Long lastRecordCount = 0L;

    // ============ SCHEDULED JOBS ============

    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduleProcessAt8Am() {
        log.info("========== START: SMS Processing at 8:00 AM (Initial COB Check) ==========");
        processWithRetry("8:00 AM");
        log.info("========== END: SMS Processing at 8:00 AM ==========");
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void scheduleProcessAt9Am() {
        log.info("========== START: SMS Processing at 9:00 AM (Retry) ==========");
        processWithRetry("9:00 AM");
        log.info("========== END: SMS Processing at 9:00 AM ==========");
    }

    @Scheduled(cron = "0 0 10 * * ?")
    public void scheduleProcessAt10Am() {
        log.info("========== START: SMS Processing at 10:00 AM (Retry) ==========");
        processWithRetry("10:00 AM");
        log.info("========== END: SMS Processing at 10:00 AM ==========");
    }

    @Scheduled(cron = "0 0 11 * * ?")
    public void scheduleProcessAt11Am() {
        log.info("========== START: SMS Processing at 11:00 AM (Retry) ==========");
        processWithRetry("11:00 AM");
        log.info("========== END: SMS Processing at 11:00 AM ==========");
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void scheduleProcessAt12Pm() {
        log.info("========== START: SMS Processing at 12:00 PM (Retry) ==========");
        processWithRetry("12:00 PM");
        log.info("========== END: SMS Processing at 12:00 PM ==========");
    }

    @Scheduled(cron = "0 0 13 * * ?")
    public void scheduleProcessAt1Pm() {
        log.info("========== START: SMS Processing at 1:00 PM (Retry) ==========");
        processWithRetry("1:00 PM");
        log.info("========== END: SMS Processing at 1:00 PM ==========");
    }

    @Scheduled(cron = "0 0 14 * * ?")
    public void scheduleProcessAt2Pm() {
        log.info("========== START: SMS Processing at 2:00 PM (Retry) ==========");
        processWithRetry("2:00 PM");
        log.info("========== END: SMS Processing at 2:00 PM ==========");
    }

    @Scheduled(cron = "0 0 15 * * ?")
    public void scheduleProcessAt3Pm() {
        log.info("========== START: SMS Processing at 3:00 PM (Retry) ==========");
        processWithRetry("3:00 PM");
        log.info("========== END: SMS Processing at 3:00 PM ==========");
    }

    @Scheduled(cron = "0 0 16 * * ?")
    public void scheduleProcessAt4Pm() {
        log.info("========== START: SMS Processing at 4:00 PM (Retry) ==========");
        processWithRetry("4:00 PM");
        log.info("========== END: SMS Processing at 4:00 PM ==========");
    }

    @Scheduled(cron = "0 0 17 * * ?")
    public void scheduleProcessAt5Pm() {
        log.info("========== START: SMS Processing at 5:00 PM (Retry) ==========");
        processWithRetry("5:00 PM");
        log.info("========== END: SMS Processing at 5:00 PM ==========");
    }

    @Scheduled(cron = "0 0 18 * * ?")
    public void scheduleProcessAt6Pm() {
        log.info("========== START: SMS Processing at 6:00 PM (Retry) ==========");
        processWithRetry("6:00 PM");
        log.info("========== END: SMS Processing at 6:00 PM ==========");
    }

    @Scheduled(cron = "0 0 19 * * ?")
    public void scheduleProcessAt7Pm() {
        log.info("========== START: SMS Processing at 7:00 PM (Retry) ==========");
        processWithRetry("7:00 PM");
        log.info("========== END: SMS Processing at 7:00 PM ==========");
    }

    @Scheduled(cron = "0 0 20 * * ?")
    public void scheduleProcessAt8Pm() {
        log.info("========== START: SMS Processing at 8:00 PM (Retry) ==========");
        processWithRetry("8:00 PM");
        log.info("========== END: SMS Processing at 8:00 PM ==========");
    }

    @Scheduled(cron = "0 0 21 * * ?")
    public void scheduleProcessAt9Pm() {
        log.info("========== START: SMS Processing at 9:00 PM (Retry) ==========");
        processWithRetry("9:00 PM");
        log.info("========== END: SMS Processing at 9:00 PM ==========");
    }

    @Scheduled(cron = "0 0 22 * * ?")
    public void scheduleProcessAt10Pm() {
        log.info("========== START: SMS Processing at 10:00 PM (Retry) ==========");
        processWithRetry("10:00 PM");
        log.info("========== END: SMS Processing at 10:00 PM ==========");
    }

    @Scheduled(cron = "0 0 23 * * ?")
    public void scheduleProcessAt11Pm() {
        log.info("========== START: SMS Processing at 11:00 PM (Final Retry) ==========");
        processWithRetry("11:00 PM");
        log.info("========== END: SMS Processing at 11:00 PM ==========");
    }

    // ============ MAIN PROCESSING LOGIC ============

    private void processWithRetry(String timeLabel) {
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
            notificationService.processPendingSmsNotifications();

            // Update tracking after processing
            lastProcessingTime = LocalDateTime.now();

            // STEP 3: Retry failed records
            log.info("STEP 2: Retrying failed records ({})", timeLabel);
            retrySmsForFailedRecords(reportDate);

            // STEP 4: Check completion status after retry
            if (isProcessingComplete(reportDate)) {
                log.info("✓ All SMS processing complete at {}", timeLabel);
            }

        } catch (Exception e) {
            log.error("✗ Exception in SMS Processing at {}: {} | Cause: {}",
                    timeLabel, e.getMessage(), e.getCause(), e);
        }
    }

    // ============ COMPLETION CHECK ============

    private boolean isProcessingComplete(LocalDate reportDate) {
        try {
            // Check 1: No failures in SmsFailureLog
            List<SmsFailureLog> failedRecords = smsFailureLogRepository.findFailedRecordsByReportDate(reportDate);

            if (!failedRecords.isEmpty()) {
                log.info("Processing NOT complete: {} failures still exist", failedRecords.size());
                return false;
            }

            // Check 2: We must have processed at least once (lastProcessingTime set)
            if (lastProcessingTime == null) {
                log.info("Processing NOT complete: No records processed yet (COB might not be finished)");
                return false;
            }

            // Check 3: Enough time has passed since last processing (to ensure COB won't send more data)
            LocalDateTime thresholdTime = lastProcessingTime.minusMinutes(PROCESSING_COMPLETE_THRESHOLD_MINUTES);
            if (LocalDateTime.now().isBefore(thresholdTime.plusMinutes(PROCESSING_COMPLETE_THRESHOLD_MINUTES))) {
                log.info("Processing NOT complete: Not enough time passed since last processing");
                return false;
            }

            log.info("✓ Processing COMPLETE: Zero failures + time threshold passed");
            return true;

        } catch (Exception e) {
            log.error("✗ Error checking completion status: {}", e.getMessage(), e);
            return false; // If error, assume not complete (safer)
        }
    }

    // ============ RETRY LOGIC ============

    private void retrySmsForFailedRecords(LocalDate reportDate) {
        try {
            String messageContent = null;

            try {
                messageContent = notificationService.getMessageContent();
            } catch (Exception e) {
                log.warn("Could not load message content, retrying with null: {}", e.getMessage());
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

                    // Attempt to send
                    notificationService.sendSmsDirectly(failureLog.getPhoneNumber(), messageContent);
                    retrySuccessCount++;

                    // Update to SUCCESS
                    failureLog.setStatus(SMS_STATUS_SUCCESS);
                    failureLog.setLastRetryDate(LocalDateTime.now());
                    smsFailureLogRepository.save(failureLog);

                    log.info("  ✓ Retry successful for phone: {}", failureLog.getPhoneNumber());

                } catch (Exception e) {
                    retryFailureCount++;
                    log.error("  ✗ Retry failed for phone: {} | Error: {}",
                            failureLog.getPhoneNumber(), e.getMessage());

                    // Update retry count and reason
                    failureLog.setRetryCount(failureLog.getRetryCount() + 1);
                    failureLog.setFailureReason(e.getMessage());
                    failureLog.setLastRetryDate(LocalDateTime.now());
                    failureLog.setStatus(SMS_STATUS_FAILURE);
                    smsFailureLogRepository.save(failureLog);

                    log.info("  Retry count now: {}", failureLog.getRetryCount());
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
