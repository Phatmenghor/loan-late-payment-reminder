package com.backend.features.sms.service;

import com.backend.features.sms.dto.BatchProcessingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class BatchProcessingStatusService {

    private volatile BatchProcessingStatus currentStatus = null;
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    public void startProcessing(int totalRecords) {
        processedCount.set(0);
        successCount.set(0);
        failureCount.set(0);
        stopRequested.set(false);

        this.currentStatus = BatchProcessingStatus.builder()
                .totalRecords(totalRecords)
                .processedCount(0)
                .successCount(0)
                .failureCount(0)
                .progressPercentage(0.0)
                .status("PROCESSING")
                .message("SMS batch processing is in progress")
                .startedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void updateProgress(boolean success) {
        if (currentStatus == null) {
            return;
        }

        processedCount.incrementAndGet();
        if (success) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }

        int total = currentStatus.getTotalRecords();
        int processed = processedCount.get();
        double percentage = total > 0 ? (processed * 100.0) / total : 0.0;

        currentStatus.setProcessedCount(processed);
        currentStatus.setSuccessCount(successCount.get());
        currentStatus.setFailureCount(failureCount.get());
        currentStatus.setProgressPercentage(Math.round(percentage * 100.0) / 100.0);
        currentStatus.setUpdatedAt(LocalDateTime.now());
    }

    public void completeProcessing() {
        if (currentStatus == null) {
            return;
        }

        if (stopRequested.get()) {
            currentStatus.setStatus("STOPPED");
            currentStatus.setMessage("SMS batch processing was stopped before completion");
        } else {
            currentStatus.setStatus("COMPLETED");
            currentStatus.setMessage("SMS batch processing completed successfully");
        }
        currentStatus.setUpdatedAt(LocalDateTime.now());

        log.info("Batch processing finished - Status: {}, Total: {}, Success: {}, Failure: {}",
                currentStatus.getStatus(), currentStatus.getTotalRecords(), successCount.get(), failureCount.get());
    }

    public BatchProcessingStatus stopProcessing() {
        stopRequested.set(true);
        if (currentStatus != null) {
            currentStatus.setStatus("STOPPED");
            currentStatus.setMessage("SMS batch processing stopped by user request");
            currentStatus.setUpdatedAt(LocalDateTime.now());
        }
        log.warn("Batch SMS processing stopped by user request");
        return getStatus();
    }

    public boolean isStopRequested() {
        return stopRequested.get();
    }

    public void failProcessing(String errorMessage) {
        if (currentStatus == null) {
            return;
        }

        currentStatus.setStatus("FAILED");
        currentStatus.setMessage("SMS batch processing failed: " + errorMessage);
        currentStatus.setUpdatedAt(LocalDateTime.now());

        log.error("Batch processing failed - {}", errorMessage);
    }

    public BatchProcessingStatus getStatus() {
        return currentStatus != null ? currentStatus : getDefaultStatus();
    }

    private BatchProcessingStatus getDefaultStatus() {
        return BatchProcessingStatus.builder()
                .totalRecords(0)
                .processedCount(0)
                .successCount(0)
                .failureCount(0)
                .progressPercentage(0.0)
                .status("IDLE")
                .message("No batch processing in progress")
                .build();
    }
}
