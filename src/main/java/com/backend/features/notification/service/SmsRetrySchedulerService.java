package com.backend.features.notification.service;

import com.backend.features.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsRetrySchedulerService {

    private final NotificationServiceImpl notificationService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduleProcessAt8Am() {
        log.info("========== START: SMS Processing at 8:00 AM ==========");
        notificationService.processPendingSmsNotifications();
        log.info("========== END: SMS Processing at 8:00 AM ==========");
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void scheduleProcessAt9Am() {
        log.info("========== START: SMS Processing at 9:00 AM ==========");
        notificationService.processPendingSmsNotifications();
        log.info("========== END: SMS Processing at 9:00 AM ==========");
    }

    @Scheduled(cron = "0 0 10 * * ?")
    public void scheduleProcessAt10Am() {
        log.info("========== START: SMS Processing at 10:00 AM ==========");
        notificationService.processPendingSmsNotifications();
        log.info("========== END: SMS Processing at 10:00 AM ==========");
    }

    @Scheduled(cron = "0 0 11 * * ?")
    public void scheduleProcessAt11Am() {
        log.info("========== START: SMS Processing at 11:00 AM ==========");
        notificationService.processPendingSmsNotifications();
        log.info("========== END: SMS Processing at 11:00 AM ==========");
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void scheduleProcessAt12Pm() {
        log.info("========== START: SMS Processing at 12:00 PM ==========");
        notificationService.processPendingSmsNotifications();
        log.info("========== END: SMS Processing at 12:00 PM ==========");
    }
}
