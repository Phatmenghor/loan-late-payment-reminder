package com.backend.features.notification.service;

import com.backend.features.notification.repository.NotificationQueueRepository;
import com.backend.features.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRetrySchedulerService {

    private final NotificationServiceImpl notificationService;
    private final NotificationQueueRepository notificationQueueRepository;

    private static final String CAMBODIA_TIMEZONE = "Asia/Phnom_Penh";

    @Scheduled(cron = "0 0 0 * * ?", zone = CAMBODIA_TIMEZONE)
    @Transactional
    public void cleanupQueueRecords() {
        log.info("========== START: Notification queue cleanup at midnight ==========");
        try {
            int deletedCount = notificationQueueRepository.deleteAllRecords();
            log.info("Queue cleanup complete: {} records deleted", deletedCount);
        } catch (Exception e) {
            log.error("Queue cleanup failed: {}", e.getMessage(), e);
        }
        log.info("========== END: Notification queue cleanup ==========");
    }

    @Scheduled(cron = "0 0 8 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt8Am() {
        log.info("========== START: Notification processing at 8:00 AM ==========");
        notificationService.processWithRetry("8:00 AM");
        log.info("========== END: Notification processing at 8:00 AM ==========");
    }

    @Scheduled(cron = "0 0 9 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt9Am() {
        log.info("========== START: Notification processing at 9:00 AM ==========");
        notificationService.processWithRetry("9:00 AM");
        log.info("========== END: Notification processing at 9:00 AM ==========");
    }

    @Scheduled(cron = "0 0 10 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt10Am() {
        log.info("========== START: Notification processing at 10:00 AM ==========");
        notificationService.processWithRetry("10:00 AM");
        log.info("========== END: Notification processing at 10:00 AM ==========");
    }

    @Scheduled(cron = "0 0 11 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt11Am() {
        log.info("========== START: Notification processing at 11:00 AM ==========");
        notificationService.processWithRetry("11:00 AM");
        log.info("========== END: Notification processing at 11:00 AM ==========");
    }

    @Scheduled(cron = "0 0 12 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt12Pm() {
        log.info("========== START: Notification processing at 12:00 PM ==========");
        notificationService.processWithRetry("12:00 PM");
        log.info("========== END: Notification processing at 12:00 PM ==========");
    }

    @Scheduled(cron = "0 0 13 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt1Pm() {
        log.info("========== START: Notification processing at 1:00 PM ==========");
        notificationService.processWithRetry("1:00 PM");
        log.info("========== END: Notification processing at 1:00 PM ==========");
    }

    @Scheduled(cron = "0 0 14 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt2Pm() {
        log.info("========== START: Notification processing at 2:00 PM ==========");
        notificationService.processWithRetry("2:00 PM");
        log.info("========== END: Notification processing at 2:00 PM ==========");
    }

    @Scheduled(cron = "0 0 15 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt3Pm() {
        log.info("========== START: Notification processing at 3:00 PM ==========");
        notificationService.processWithRetry("3:00 PM");
        log.info("========== END: Notification processing at 3:00 PM ==========");
    }

    @Scheduled(cron = "0 0 16 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt4Pm() {
        log.info("========== START: Notification processing at 4:00 PM ==========");
        notificationService.processWithRetry("4:00 PM");
        log.info("========== END: Notification processing at 4:00 PM ==========");
    }

    @Scheduled(cron = "0 0 17 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt5Pm() {
        log.info("========== START: Notification processing at 5:00 PM ==========");
        notificationService.processWithRetry("5:00 PM");
        log.info("========== END: Notification processing at 5:00 PM ==========");
    }

    @Scheduled(cron = "0 0 18 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt6Pm() {
        log.info("========== START: Notification processing at 6:00 PM ==========");
        notificationService.processWithRetry("6:00 PM");
        log.info("========== END: Notification processing at 6:00 PM ==========");
    }

    @Scheduled(cron = "0 0 19 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt7Pm() {
        log.info("========== START: Notification processing at 7:00 PM ==========");
        notificationService.processWithRetry("7:00 PM");
        log.info("========== END: Notification processing at 7:00 PM ==========");
    }

    @Scheduled(cron = "0 0 20 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt8Pm() {
        log.info("========== START: Notification processing at 8:00 PM ==========");
        notificationService.processWithRetry("8:00 PM");
        log.info("========== END: Notification processing at 8:00 PM ==========");
    }

    @Scheduled(cron = "0 0 21 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt9Pm() {
        log.info("========== START: Notification processing at 9:00 PM ==========");
        notificationService.processWithRetry("9:00 PM");
        log.info("========== END: Notification processing at 9:00 PM ==========");
    }

    @Scheduled(cron = "0 0 22 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt10Pm() {
        log.info("========== START: Notification processing at 10:00 PM ==========");
        notificationService.processWithRetry("10:00 PM");
        log.info("========== END: Notification processing at 10:00 PM ==========");
    }

    @Scheduled(cron = "0 0 23 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleProcessAt11Pm() {
        log.info("========== START: Notification processing at 11:00 PM ==========");
        notificationService.processWithRetry("11:00 PM");
        log.info("========== END: Notification processing at 11:00 PM ==========");
    }
}
