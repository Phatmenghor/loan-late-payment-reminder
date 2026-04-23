package com.backend.features.src.main.java.com.cpbank;

import com.cpbank.service.SendNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class SmsLoanLateApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SmsLoanLateApplication.class);

    @Autowired
    private final SendNotificationService sendNotificationService;

    public static void main(String[] args) {
        SpringApplication.run(SmsLoanLateApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Initial call to the service
        sendNotificationService.pushNotificationsToMobile();
    }

    //@Scheduled(fixedRate = 60000) // 1 minute = 60,000 milliseconds
    @Scheduled(fixedRate = 10000) // 10 seconds = 10,000 milliseconds
    public void scheduleTask() {
        try {
            sendNotificationService.pushNotificationsToMobile();
            logger.info("Service is called.");
        } catch (Exception e) {
            logger.error("Error in scheduled task: " + e.getMessage());
        }
    }
}