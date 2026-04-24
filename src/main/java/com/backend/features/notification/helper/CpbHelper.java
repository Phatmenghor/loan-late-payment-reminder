package com.backend.features.notification.helper;

import com.backend.features.notification.repository.SmsConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CpbHelper {

    private final SmsConfigRepository smsConfigRepository;
    private static final String DEFAULT_MESSAGE = "Loan payment reminder";

    public String getContentDescription() {
        try {
            return smsConfigRepository
                    .findActiveByConfigType("SMS_LOAN_LATE")
                    .map(config -> {
                        log.info("SMS message content loaded from PostgreSQL");
                        return config.getConfigValue();
                    })
                    .orElseGet(() -> {
                        log.warn("SMS_LOAN_LATE config not found in database, using default message");
                        return DEFAULT_MESSAGE;
                    });
        } catch (Exception e) {
            log.error("Error fetching SMS message content from database: {}", e.getMessage(), e);
            return DEFAULT_MESSAGE;
        }
    }
}
