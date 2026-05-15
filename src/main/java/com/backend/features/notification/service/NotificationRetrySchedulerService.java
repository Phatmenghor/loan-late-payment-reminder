//package com.backend.features.notification.service;
//
//import com.backend.features.notification.service.impl.NotificationServiceImpl;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationRetrySchedulerService {
//
//    private final NotificationServiceImpl notificationService;
//
//    // ============ SCHEDULED JOBS ============
//
//    @Scheduled(cron = "0 0 8 * * ?")
//    public void scheduleProcessAt8Am() {
//        log.info("========== START: SMS Processing at 8:00 AM (Initial COB Check) ==========");
//        notificationService.processWithRetry("8:00 AM");
//        log.info("========== END: SMS Processing at 8:00 AM ==========");
//    }
//
//    @Scheduled(cron = "0 0 9 * * ?")
//    public void scheduleProcessAt9Am() {
//        log.info("========== START: SMS Processing at 9:00 AM (Retry) ==========");
//        notificationService.processWithRetry("9:00 AM");
//        log.info("========== END: SMS Processing at 9:00 AM ==========");
//    }
//
//    @Scheduled(cron = "0 0 10 * * ?")
//    public void scheduleProcessAt10Am() {
//        log.info("========== START: SMS Processing at 10:00 AM (Retry) ==========");
//        notificationService.processWithRetry("10:00 AM");
//        log.info("========== END: SMS Processing at 10:00 AM ==========");
//    }
//
//    @Scheduled(cron = "0 0 11 * * ?")
//    public void scheduleProcessAt11Am() {
//        log.info("========== START: SMS Processing at 11:00 AM (Retry) ==========");
//        notificationService.processWithRetry("11:00 AM");
//        log.info("========== END: SMS Processing at 11:00 AM ==========");
//    }
//
//    @Scheduled(cron = "0 0 12 * * ?")
//    public void scheduleProcessAt12Pm() {
//        log.info("========== START: SMS Processing at 12:00 PM (Retry) ==========");
//        notificationService.processWithRetry("12:00 PM");
//        log.info("========== END: SMS Processing at 12:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 13 * * ?")
//    public void scheduleProcessAt1Pm() {
//        log.info("========== START: SMS Processing at 1:00 PM (Retry) ==========");
//        notificationService.processWithRetry("1:00 PM");
//        log.info("========== END: SMS Processing at 1:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 14 * * ?")
//    public void scheduleProcessAt2Pm() {
//        log.info("========== START: SMS Processing at 2:00 PM (Retry) ==========");
//        notificationService.processWithRetry("2:00 PM");
//        log.info("========== END: SMS Processing at 2:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 15 * * ?")
//    public void scheduleProcessAt3Pm() {
//        log.info("========== START: SMS Processing at 3:00 PM (Retry) ==========");
//        notificationService.processWithRetry("3:00 PM");
//        log.info("========== END: SMS Processing at 3:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 16 * * ?")
//    public void scheduleProcessAt4Pm() {
//        log.info("========== START: SMS Processing at 4:00 PM (Retry) ==========");
//        notificationService.processWithRetry("4:00 PM");
//        log.info("========== END: SMS Processing at 4:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 17 * * ?")
//    public void scheduleProcessAt5Pm() {
//        log.info("========== START: SMS Processing at 5:00 PM (Retry) ==========");
//        notificationService.processWithRetry("5:00 PM");
//        log.info("========== END: SMS Processing at 5:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 18 * * ?")
//    public void scheduleProcessAt6Pm() {
//        log.info("========== START: SMS Processing at 6:00 PM (Retry) ==========");
//        notificationService.processWithRetry("6:00 PM");
//        log.info("========== END: SMS Processing at 6:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 19 * * ?")
//    public void scheduleProcessAt7Pm() {
//        log.info("========== START: SMS Processing at 7:00 PM (Retry) ==========");
//        notificationService.processWithRetry("7:00 PM");
//        log.info("========== END: SMS Processing at 7:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 20 * * ?")
//    public void scheduleProcessAt8Pm() {
//        log.info("========== START: SMS Processing at 8:00 PM (Retry) ==========");
//        notificationService.processWithRetry("8:00 PM");
//        log.info("========== END: SMS Processing at 8:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 21 * * ?")
//    public void scheduleProcessAt9Pm() {
//        log.info("========== START: SMS Processing at 9:00 PM (Retry) ==========");
//        notificationService.processWithRetry("9:00 PM");
//        log.info("========== END: SMS Processing at 9:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 22 * * ?")
//    public void scheduleProcessAt10Pm() {
//        log.info("========== START: SMS Processing at 10:00 PM (Retry) ==========");
//        notificationService.processWithRetry("10:00 PM");
//        log.info("========== END: SMS Processing at 10:00 PM ==========");
//    }
//
//    @Scheduled(cron = "0 0 23 * * ?")
//    public void scheduleProcessAt11Pm() {
//        log.info("========== START: SMS Processing at 11:00 PM (Final Retry) ==========");
//        notificationService.processWithRetry("11:00 PM");
//        log.info("========== END: SMS Processing at 11:00 PM ==========");
//    }
//}
