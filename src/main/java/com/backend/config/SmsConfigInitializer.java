package com.backend.config;

import com.backend.features.notification.models.SmsConfig;
import com.backend.features.notification.repository.SmsConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsConfigInitializer implements CommandLineRunner {

    private final SmsConfigRepository smsConfigRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeSmsConfigs();
    }

    private void initializeSmsConfigs() {
        String smsLoanLateType = "SMS_LOAN_LATE";

        if (smsConfigRepository.findByConfigType(smsLoanLateType).isEmpty()) {
            String khmerMessage = "ធនាគារប្រៃសណីយ៍កម្ពុជា ក.អ សូមស្វាគមន៍! សូមលោកអ្នកអញ្ជើញមកបង់ប្រាក់ឲ្យបានទាន់ពេលតាមតារាងសងប្រាក់របស់លោកអ្នក។ សូមអរគុណ 070 200 002";

            SmsConfig smsConfig = SmsConfig.builder()
                    .configType(smsLoanLateType)
                    .configValue(khmerMessage)
                    .isActive(true)
                    .build();

            smsConfigRepository.save(smsConfig);
            log.info("Initialized SMS_LOAN_LATE configuration");
        }
    }
}
