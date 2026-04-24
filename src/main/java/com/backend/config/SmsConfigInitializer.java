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

        if (notificationConfigRepository.findByConfigType(notificationLoanLateType).isEmpty()) {
            String khmerMessage = "ធនាគារប្រៃសណីយ៍កម្ពុជា ក.អ សូមស្វាគមន៍! សូមលោកអ្នកអញ្ជើញមកបង់ប្រាក់ឲ្យបានទាន់ពេលតាមតារាងសងប្រាក់របស់លោកអ្នក។ សូមអរគុណ 070 200 002";

            NotificationConfig notificationConfig = NotificationConfig.builder()
                    .configType(notificationLoanLateType)
                    .configValue(khmerMessage)
                    .isActive(true)
                    .build();

            notificationConfigRepository.save(notificationConfig);
            log.info("Initialized NOTIFICATION_LOAN_LATE configuration");
        }
    }
}
