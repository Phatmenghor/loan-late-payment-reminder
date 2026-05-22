package com.backend.config;

import com.backend.features.auth.model.AdConfig;
import com.backend.features.auth.repository.AdConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdConfigInitializer implements CommandLineRunner {

    private final AdConfigRepository adConfigRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeAdConfig();
    }

    private void initializeAdConfig() {
        if (adConfigRepository.findFirstByOrderByCreatedAtAsc().isEmpty()) {
            AdConfig adConfig = AdConfig.builder()
                    .adEnabled(true)
                    .description("Active Directory authentication config. Set ad_enabled=false to bypass AD and allow all logins.")
                    .build();

            adConfigRepository.save(adConfig);
            log.info("Initialized AD configuration with ad_enabled=true");
        } else {
            log.info("AD configuration already exists, skipping initialization");
        }
    }
}
