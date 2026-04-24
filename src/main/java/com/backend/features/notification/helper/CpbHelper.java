package com.backend.features.notification.helper;

import com.backend.features.notification.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CpbHelper {

    private final NotificationConfigRepository notificationConfigRepository;
    private static final String DEFAULT_MESSAGE = "Loan payment reminder";

    public String getContentDescription() {
        try {
            return notificationConfigRepository
                    .findActiveByConfigType("NOTIFICATION_LOAN_LATE")
                    .map(config -> {
                        log.info("Notification message content loaded from PostgreSQL");
                        return config.getConfigValue();
                    })
                    .orElseGet(() -> {
                        log.warn("NOTIFICATION_LOAN_LATE config not found in database, using default message");
                        return DEFAULT_MESSAGE;
                    });
        } catch (Exception e) {
            log.error("Error fetching notification message content from database: {}", e.getMessage(), e);
            return DEFAULT_MESSAGE;
        }
    }
}
