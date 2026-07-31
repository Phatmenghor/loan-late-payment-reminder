package com.backend.config;

import com.backend.features.notification.models.NotificationConfig;
import com.backend.features.notification.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsConfigInitializer implements CommandLineRunner {

    private final NotificationConfigRepository notificationConfigRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeNotificationConfigs();
    }

    private void initializeNotificationConfigs() {
        String notificationLoanLateType = "NOTIFICATION_LOAN_LATE";
        String notificationSmsType = "NOTIFICATION_SMS";

        String loanLateMessage = "ធនាគារប្រៃសណីយ៍កម្ពុជា ក.អ សូមស្វាគមន៍! សូមលោកអ្នកអញ្ជើញមកបង់ប្រាក់ឲ្យបានទាន់ពេលតាមតារាងសងប្រាក់របស់លោកអ្នក។ សូមអរគុណ 070 200 002";
        String smsMessage = "ធនាគារប្រៃសណីយ៍កម្ពុជា ក.អ សូមស្វាគមន៍! សូមលោកអ្នកអញ្ជើញមកបង់ប្រាក់ឲ្យបានទាន់ពេលតាមតារាងសងប្រាក់របស់លោកអ្នក។ សូមអរគុណ 070 200 002";

        // Initialize NOTIFICATION_LOAN_LATE
        if (notificationConfigRepository.findByConfigType(notificationLoanLateType).isEmpty()) {
            NotificationConfig notificationConfig = NotificationConfig.builder()
                    .configType(notificationLoanLateType)
                    .configValue(loanLateMessage)
                    .description("Notification template message for internal service")
                    .isActive(true)
                    .build();

            notificationConfigRepository.save(notificationConfig);
            log.info("Initialized NOTIFICATION_LOAN_LATE configuration");
        }

        // Initialize NOTIFICATION_SMS for batch SMS sending
        if (notificationConfigRepository.findByConfigType(notificationSmsType).isEmpty()) {
            NotificationConfig smsConfig = NotificationConfig.builder()
                    .configType(notificationSmsType)
                    .configValue(smsMessage)
                    .description("SMS template message for bulk SMS")
                    .isActive(true)
                    .build();

            notificationConfigRepository.save(smsConfig);
            log.info("Initialized NOTIFICATION_SMS configuration");
        }
    }
}
