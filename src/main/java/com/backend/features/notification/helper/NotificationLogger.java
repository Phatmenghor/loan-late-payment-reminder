package com.backend.features.notification.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class NotificationLogger {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void logProcessStart() {
        log.info("========== START: Processing pending SMS notifications ==========");
    }

    public void logProcessEnd(int successCount, int failureCount) {
        log.info("========== RESULT: Success: {}, Failed: {} ==========", successCount, failureCount);
        log.info("========== END: Processing pending SMS notifications ==========");
    }

    public void logPendingSmsFetched(int count) {
        log.info("✓ Found {} pending SMS notifications to process", count);
    }

    public void logNoPendingSms() {
        log.info("✓ No pending SMS notifications found");
    }

    public void logSmsProcessStart(String phoneNumber) {
        log.info("========== START: Processing SMS for phone: {} ==========", phoneNumber);
    }

    public void logSmsProcessEnd(String phoneNumber) {
        log.info("========== END: SMS processing completed for phone: {} ==========", phoneNumber);
    }

    public void logDatabaseSelect(String query) {
        log.debug("DATABASE [SELECT]: {}", query);
    }

    public void logDatabaseInsert(String phoneNumber) {
        log.debug("DATABASE [INSERT]: phone_number={}, timestamp={}", phoneNumber, getCurrentTime());
    }

    public void logDatabaseUpdate(String phoneNumber, String status) {
        log.debug("DATABASE [UPDATE]: phone_number={}, sms_status={}, timestamp={}", phoneNumber, status, getCurrentTime());
    }

    public void logApiSend(String phoneNumber, String apiEndpoint) {
        log.info("API [REQUEST]: Sending SMS to phone={} | Endpoint={}", phoneNumber, apiEndpoint);
    }

    public void logApiResponse(String phoneNumber, String status) {
        log.info("API [RESPONSE]: phone={} | status={}", phoneNumber, status);
    }

    public void logApiError(String phoneNumber, String errorMessage) {
        log.error("API [ERROR]: phone={} | error={}", phoneNumber, errorMessage);
    }

    public void logSmsSuccess(String phoneNumber, String status) {
        log.info("✓ SMS sent successfully | phone={} | status={}", phoneNumber, status);
    }

    public void logSmsFailure(String phoneNumber, String errorMessage) {
        log.error("✗ SMS sending failed | phone={} | error={}", phoneNumber, errorMessage);
    }

    public void logSmsStatusUpdate(String phoneNumber, String newStatus) {
        log.info("✓ SMS status updated | phone={} | newStatus={}", phoneNumber, newStatus);
    }

    public void logValidationError(String message) {
        log.warn("⚠ Validation error: {}", message);
    }

    public void logTransactionStart(String transactionId) {
        log.debug("TRANSACTION [START]: {}", transactionId);
    }

    public void logTransactionEnd(String transactionId) {
        log.debug("TRANSACTION [END]: {}", transactionId);
    }

    public void logException(String context, Exception e) {
        log.error("✗ Exception in {}: {} | Cause: {}", context, e.getMessage(), e.getCause(), e);
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }
}
