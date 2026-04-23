package com.backend.features.notification.service;

import com.backend.features.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsSchedulerService {

    private final NotificationServiceImpl notificationService;

    @Scheduled(cron = "0 0 13 * * ?")
    public void processPendingSmsDaily() {
        log.info("========== SCHEDULED: Daily SMS Processing Started at 1:00 PM ==========");
        try {
            notificationService.processPendingSmsNotifications();
            log.info("========== SCHEDULED: Daily SMS Processing Completed Successfully ==========");
        } catch (Exception e) {
            log.error("========== SCHEDULED: Daily SMS Processing Failed ==========", e);
        }
    }
}
