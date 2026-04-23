package com.backend.features.notification.service;

import com.backend.features.notification.helper.CpbHelper;
import com.backend.features.notification.models.SmsFailureLog;
import com.backend.features.notification.repository.SmsFailureLogRepository;
import com.backend.features.notification.repository.SmsLogRepository;
import com.backend.features.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsRetrySchedulerService {

    private static final String SMS_STATUS_SUCCESS = "SVC-SUCCESS-00";
    private static final String SMS_STATUS_FAILED = "SVC-FAILED";

    private final SmsFailureLogRepository smsFailureLogRepository;
    private final SmsLogRepository smsLogRepository;
    private final RestTemplate restTemplate;
    private final CpbHelper cpbHelper;
    private final NotificationServiceImpl notificationService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduleProcessAt8Am() {
        log.info("========== START: SMS Processing at 8:00 AM ==========");
        notificationService.processPendingSmsNotifications();
        retrySmsForFailedRecords();
        log.info("========== END: SMS Processing at 8:00 AM ==========");
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void scheduleProcessAt9Am() {
        log.info("========== START: SMS Processing at 9:00 AM ==========");
        notificationService.processPendingSmsNotifications();
        retrySmsForFailedRecords();
        log.info("========== END: SMS Processing at 9:00 AM ==========");
    }

    @Scheduled(cron = "0 0 10 * * ?")
    public void scheduleProcessAt10Am() {
        log.info("========== START: SMS Processing at 10:00 AM ==========");
        notificationService.processPendingSmsNotifications();
        retrySmsForFailedRecords();
        log.info("========== END: SMS Processing at 10:00 AM ==========");
    }

    @Scheduled(cron = "0 0 11 * * ?")
    public void scheduleProcessAt11Am() {
        log.info("========== START: SMS Processing at 11:00 AM ==========");
        notificationService.processPendingSmsNotifications();
        retrySmsForFailedRecords();
        log.info("========== END: SMS Processing at 11:00 AM ==========");
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void scheduleProcessAt12Pm() {
        log.info("========== START: SMS Processing at 12:00 PM ==========");
        notificationService.processPendingSmsNotifications();
        retrySmsForFailedRecords();
        log.info("========== END: SMS Processing at 12:00 PM ==========");
    }

    private void retrySmsForFailedRecords() {
        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);
            String messageContent = cpbHelper.getContentDescription();

            List<SmsFailureLog> failedRecords = smsFailureLogRepository.findFailedRecordsByReportDate(reportDate);

            if (failedRecords.isEmpty()) {
                log.info("✓ All SMS sent successfully for date: {} - No failures to retry", reportDate);
                return;
            }

            log.info("✓ Found {} failed SMS records to retry for date: {}", failedRecords.size(), reportDate);
            log.info("   Processing only failures - skipping view query to avoid re-processing 20K+ records");

            int successCount = 0;
            int stillFailedCount = 0;

            for (SmsFailureLog failureLog : failedRecords) {
                try {
                    log.info("========== START: Retrying SMS for phone: {} | Retry count: {} ==========",
                            failureLog.getPhoneNumber(), failureLog.getRetryCount());

                    notificationService.sendSmsDirectly(failureLog.getPhoneNumber(), messageContent);
                    successCount++;

                    failureLog.setStatus(SMS_STATUS_SUCCESS);
                    failureLog.setLastRetryDate(LocalDateTime.now());
                    smsFailureLogRepository.save(failureLog);

                    log.info("✓ SMS retry successful | Phone: {} | Total retries: {}",
                            failureLog.getPhoneNumber(), failureLog.getRetryCount());
                    log.info("========== END: SMS retry completed for phone: {} ==========", failureLog.getPhoneNumber());

                } catch (Exception e) {
                    stillFailedCount++;
                    log.error("✗ SMS retry still failed | Phone: {} | Error: {}",
                            failureLog.getPhoneNumber(), e.getMessage());

                    failureLog.setRetryCount(failureLog.getRetryCount() + 1);
                    failureLog.setFailureReason(e.getMessage());
                    failureLog.setLastRetryDate(LocalDateTime.now());
                    smsFailureLogRepository.save(failureLog);
                }
            }

            log.info("========== RETRY RESULT: Success: {}, Still Failed: {} ==========", successCount, stillFailedCount);

        } catch (Exception e) {
            log.error("✗ Exception in SMS Retry Processing: {} | Cause: {}", e.getMessage(), e.getCause(), e);
        }
    }
}
