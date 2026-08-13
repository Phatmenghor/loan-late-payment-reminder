package com.backend.features.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdUserInactivitySchedulerService {

    private final AdAuthService adAuthService;

    private static final String CAMBODIA_TIMEZONE = "Asia/Phnom_Penh";
    private static final int DEFAULT_INACTIVITY_DAYS = 90;

    /**
     * Daily background job at 1:00 AM (Cambodia Time) to scan and lock AD user accounts
     * mapped to API keys that have not logged in for 90 days or more.
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = CAMBODIA_TIMEZONE)
    public void scheduleInactivityLockCheck() {
        log.info("========== START: Daily AD User Inactivity Lock Check (Threshold: {} days) ==========", DEFAULT_INACTIVITY_DAYS);
        try {
            int lockedCount = adAuthService.lockInactiveUsers(DEFAULT_INACTIVITY_DAYS);
            log.info("AD User Inactivity Check completed: {} account(s) locked due to inactivity", lockedCount);
        } catch (Exception e) {
            log.error("Failed during scheduled AD User Inactivity Check: {}", e.getMessage(), e);
        }
        log.info("========== END: Daily AD User Inactivity Lock Check ==========");
    }
}
