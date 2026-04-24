package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
import com.backend.config.NotificationAsyncConfig;
import com.backend.shared.constants.NotificationConstants;
import com.backend.features.notification.dto.LoanLateReminderDto;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.dto.SendNotificationRequestDto;
import com.backend.enums.common.ProcessingResult;
import com.backend.features.notification.helper.CpbHelper;
import com.backend.features.notification.helper.NotificationPayloadBuilder;
import com.backend.features.notification.helper.OracleHelper;
import com.backend.features.notification.helper.TestDataHelper;
import com.backend.features.notification.models.NotificationProcessingStatus;
import com.backend.features.notification.models.NotificationQueue;
import com.backend.features.notification.models.NotificationLog;
import com.backend.features.notification.repository.NotificationProcessingStatusRepository;
import com.backend.features.notification.repository.NotificationQueueRepository;
import com.backend.features.notification.repository.NotificationLogRepository;
import com.backend.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {


    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private final RestTemplate restTemplate;
    private final NotificationPayloadBuilder payloadBuilder;
    private final CpbApiConfig cpbApiConfig;
    private final OracleHelper oracleHelper;
    private final TestDataHelper testDataHelper;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationProcessingStatusRepository notificationProcessingStatusRepository;
    private final NotificationQueueRepository notificationQueueRepository;
    private final CpbHelper cpbHelper;
    private final NotificationAsyncConfig notificationAsyncConfig;

    @Override
    public ProcessingResult processPendingSmsNotifications() {
        log.info("BEGIN: SMS notification processing cycle");

        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);
            log.info("Target report date: {}", reportDate);

            Optional<NotificationProcessingStatus> existingStatus = notificationProcessingStatusRepository.findCompleteByReportDate(reportDate);
            if (existingStatus.isPresent()) {
                NotificationProcessingStatus status = existingStatus.get();
                log.info("CACHE HIT: {} already processed successfully", reportDate);
                log.info("Cached results: {} total, {} success, {} failure (completed at {})",
                        status.getTotalCustomers(), status.getSuccessCount(), status.getFailureCount(), status.getCompletedAt());
                return ProcessingResult.ALL_SUCCESS;
            }

            List<NotificationQueue> existingQueue = notificationQueueRepository.findByReportDate(reportDate);
            if (existingQueue.isEmpty()) {
                log.info("Queue empty for {}, fetching records", reportDate);

                String messageContent = cpbHelper.getContentDescription();
                List<LoanLateReminderDto> records = "local".equals(activeProfile) ?
                        testDataHelper.getTestLoanReminders() :
                        oracleHelper.selectLoanLateReminderRecords();

                if (records.isEmpty()) {
                    log.warn("View returned no records - COB processing may still be in progress");
                    return ProcessingResult.COB_NOT_FINISHED;
                }

                log.info("View fetch successful: {} records retrieved", records.size());
                storeRecordsToQueue(records, messageContent, reportDate);
            } else {
                log.info("Queue reuse: {} records already queued for {}", existingQueue.size(), reportDate);
            }


            int[] counts = processQueuedRecords(reportDate);
            int successCount = counts[0];
            int failureCount = counts[1];

            log.info("Delivery results: {} sent successfully, {} failed", successCount, failureCount);

            checkAndMarkCompletion(reportDate);

            if (failureCount == 0) {
                log.info("SUCCESS: All SMS delivered for {}", reportDate);
                return ProcessingResult.ALL_SUCCESS;
            } else {
                log.warn("INCOMPLETE: {} delivery failures queued for retry", failureCount);
                return ProcessingResult.WITH_FAILURES;
            }

        } catch (Exception e) {
            log.error("FAILED: Unexpected error in SMS notification cycle: {}", e.getMessage(), e);
            return ProcessingResult.WITH_FAILURES;
        }
    }

    private void storeRecordsToQueue(List<LoanLateReminderDto> records, String messageContent, LocalDate reportDate) {
        try {

            for (LoanLateReminderDto record : records) {
                String jsonPayload = payloadBuilder.buildJsonPayload(record.getPhoneNumber(), messageContent);

                NotificationQueue queueRecord = NotificationQueue.builder()
                        .customerId(record.getCustomerId())
                        .phoneNumber(record.getPhoneNumber())
                        .arrangementId(record.getArrangementId())
                        .reportDate(reportDate)
                        .messageContent(messageContent)
                        .jsonPayload(jsonPayload)
                        .status(NotificationConstants.QueueStatus.PENDING)
                        .retryCount(0)
                        .build();

                notificationQueueRepository.save(queueRecord);
            }

            log.info("Queued: {} records added with status=PENDING", records.size());
        } catch (Exception e) {
            log.error("Queue persistence failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store records to queue", e);
        }
    }

    private int[] processQueuedRecords(LocalDate reportDate) {
        try {
            List<NotificationQueue> pendingRecords = notificationQueueRepository
                    .findByStatusAndReportDate(NotificationConstants.QueueStatus.PENDING, reportDate);

            if (pendingRecords.isEmpty()) {
                return new int[]{0, 0};
            }

            log.info("Starting delivery: {} pending messages in queue", pendingRecords.size());

            String messageContent = pendingRecords.get(0).getMessageContent();
            if (messageContent == null) {
                messageContent = cpbHelper.getContentDescription();
            }

            int[] counts = processBatchAsync(pendingRecords, messageContent);
            log.info("Delivery batch complete: {} successful, {} failed", counts[0], counts[1]);

            return counts;
        } catch (Exception e) {
            log.error("Queue processing aborted: {}", e.getMessage(), e);
            return new int[]{0, 0};
        }
    }

    private int[] processBatchAsync(List<NotificationQueue> records, String messageContent) {
        int batchSize = notificationAsyncConfig.getBatchSize();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<CompletableFuture<int[]>> futures = new ArrayList<>();

        for (int i = 0; i < records.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, records.size());
            List<NotificationQueue> batch = records.subList(i, endIndex);

            CompletableFuture<int[]> future = processSingleBatchAsync(batch, messageContent);
            futures.add(future);
        }

        for (CompletableFuture<int[]> future : futures) {
            try {
                int[] counts = future.get();
                successCount.addAndGet(counts[0]);
                failureCount.addAndGet(counts[1]);
            } catch (Exception e) {
                log.error("Async batch processing error: {}", e.getMessage(), e);
                failureCount.incrementAndGet();
            }
        }

        return new int[]{successCount.get(), failureCount.get()};
    }

    @Async("notificationExecutor")
    @Transactional
    public CompletableFuture<int[]> processSingleBatchAsync(List<NotificationQueue> batch, String messageContent) {
        int success = 0;
        int failure = 0;

        for (NotificationQueue queueRecord : batch) {
            try {
                Optional<NotificationQueue> existingSuccess = notificationQueueRepository
                        .findByCustomerIdAndPhoneAndDateAndSuccess(
                                queueRecord.getCustomerId(),
                                queueRecord.getPhoneNumber(),
                                queueRecord.getReportDate());

                if (existingSuccess.isPresent()) {
                    continue;
                }

                String apiStatus = sendSmsToApi(queueRecord.getPhoneNumber(), messageContent);

                if (NotificationConstants.NotificationStatus.SUCCESS.equals(apiStatus)) {
                    updateQueueStatus(queueRecord, NotificationConstants.QueueStatus.SUCCESS, null);
                    logToPostgresSQL(queueRecord, NotificationConstants.NotificationStatus.SUCCESS, messageContent);
                    success++;
                } else {
                    updateQueueStatus(queueRecord, NotificationConstants.QueueStatus.FAILURE, "API returned failure");
                    logToPostgresSQL(queueRecord, NotificationConstants.NotificationStatus.FAILURE, messageContent);
                    failure++;
                }

            } catch (Exception e) {
                failure++;
                log.warn("Delivery failed for {}: {}", queueRecord.getPhoneNumber(), e.getMessage());
                updateQueueStatus(queueRecord, NotificationConstants.QueueStatus.FAILURE, e.getMessage());
                logToPostgresSQL(queueRecord, NotificationConstants.NotificationStatus.FAILURE, messageContent);
            }
        }

        return CompletableFuture.completedFuture(new int[]{success, failure});
    }


    private void updateQueueStatus(NotificationQueue queueRecord, String status, String failureReason) {
        int maxRetries = 3;
        int retryDelayMs = 50;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                LocalDateTime now = LocalDateTime.now();

                // Use direct UPDATE query to avoid optimistic lock conflicts
                int rowsUpdated = notificationQueueRepository.updateQueueStatusById(
                        queueRecord.getId(),
                        status,
                        now,
                        failureReason);

                if (rowsUpdated > 0) {
                    log.debug("Queue status updated successfully for {}: {} (attempt {})",
                            queueRecord.getPhoneNumber(), status, attempt);
                    return;
                } else {
                    log.warn("Queue record not found for ID: {} (phone: {})",
                            queueRecord.getId(), queueRecord.getPhoneNumber());
                    return;
                }

            } catch (OptimisticLockingFailureException e) {
                if (attempt < maxRetries) {
                    log.warn("Optimistic lock conflict for {} (attempt {}/{}), retrying...",
                            queueRecord.getPhoneNumber(), attempt, maxRetries);
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for {}: {}", queueRecord.getPhoneNumber(), ie.getMessage());
                        return;
                    }
                } else {
                    log.error("Failed to update queue status after {} attempts for {}: {}",
                            maxRetries, queueRecord.getPhoneNumber(), e.getMessage());
                }

            } catch (Exception e) {
                log.error("Failed to update queue status for {}: {}", queueRecord.getPhoneNumber(), e.getMessage(), e);
                return;
            }
        }
    }

    private void checkAndMarkCompletion(LocalDate reportDate) {
        try {
            long failureCount = notificationQueueRepository.countFailureByReportDate(reportDate);
            List<NotificationQueue> allRecords = notificationQueueRepository.findByReportDate(reportDate);
            long totalCount = allRecords.size();
            long successCount = totalCount - failureCount;

            if (failureCount == 0 && totalCount > 0) {
                Optional<NotificationProcessingStatus> existing = notificationProcessingStatusRepository.findByReportDate(reportDate);

                NotificationProcessingStatus status = existing.orElseGet(() -> NotificationProcessingStatus.builder()
                        .reportDate(reportDate)
                        .build());

                status.setTotalCustomers((int) totalCount);
                status.setSuccessCount((int) successCount);
                status.setFailureCount((int) failureCount);
                status.setIsComplete(true);
                status.setCompletedAt(LocalDateTime.now());
                status.setLastCheckedAt(LocalDateTime.now());
                status.setNotes("All SMS processed successfully from queue");

                notificationProcessingStatusRepository.save(status);
                log.info("Completion recorded: {} messages processed (all successful)", totalCount);
            } else if (failureCount > 0) {
                log.info("Completion check: {} successful, {} pending retry", successCount, failureCount);
            }

        } catch (Exception e) {
            log.error("Completion check failed: {}", e.getMessage(), e);
        }
    }

    private String sendSmsToApi(String phoneNumber, String messageContent) {
        if ("local".equals(activeProfile)) {
            log.info("SMS dispatch (LOCAL): Phone: {} | Message: {}", phoneNumber, messageContent);
            return NotificationConstants.NotificationStatus.SUCCESS;
        }

        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            log.info("SMS API call: POST {} | Phone: {}", apiUrl, phoneNumber);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null) {
                ReceptionFormatDto body = apiResponse.getBody();
                String code = body.getCode();
                String desc = body.getDesc();

                if (code != null && code.equals("0")) {
                    log.info("SMS delivered successfully to {}: {}", phoneNumber, desc);
                    return NotificationConstants.NotificationStatus.SUCCESS;
                }

                log.warn("SMS delivery failed for {}: Code={}, Desc={}", phoneNumber, code, desc);
                return NotificationConstants.NotificationStatus.FAILURE;
            }

            log.warn("SMS API error for {} | Status: {}", phoneNumber, apiResponse.getStatusCode());
            return NotificationConstants.NotificationStatus.FAILURE;

        } catch (RestClientException e) {
            log.error("SMS API call failed for {}: {}", phoneNumber, e.getMessage());
            return NotificationConstants.NotificationStatus.FAILURE;
        }
    }

    private void logToPostgresSQL(NotificationQueue queueRecord, String status, String messageContent) {
        try {
            NotificationLog smsLog = NotificationLog.builder()
                    .phoneNumber(queueRecord.getPhoneNumber())
                    .customerId(queueRecord.getCustomerId())
                    .reportDate(queueRecord.getReportDate())
                    .arrangementId(queueRecord.getArrangementId())
                    .messageContent(messageContent)
                    .jsonPayload(queueRecord.getJsonPayload())
                    .notificationStatus(status)
                    .notificationLogDate(LocalDateTime.now())
                    .build();

            notificationLogRepository.save(smsLog);

        } catch (Exception e) {
            log.error("Audit log save failed for {}: {}", queueRecord.getPhoneNumber(), e.getMessage(), e);
        }
    }

    @Override
    public String sendTestSms(SendNotificationRequestDto request) {
        log.info("Test endpoint invoked for {}", request.getPhoneNumber());

        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(request.getPhoneNumber(), request.getMessageContent());
            sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());

            NotificationLog smsLog = NotificationLog.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .messageContent(request.getMessageContent())
                    .jsonPayload(jsonPayload)
                    .notificationStatus(NotificationConstants.NotificationStatus.SUCCESS)
                    .notificationLogDate(LocalDateTime.now())
                    .build();
            notificationLogRepository.save(smsLog);

            log.info("Test SMS delivered successfully");
            return NotificationConstants.NotificationStatus.SUCCESS;

        } catch (Exception e) {
            log.error("Test SMS delivery failed: {}", e.getMessage(), e);

            String jsonPayload = payloadBuilder.buildJsonPayload(request.getPhoneNumber(), request.getMessageContent());
            NotificationLog smsLog = NotificationLog.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .messageContent(request.getMessageContent())
                    .jsonPayload(jsonPayload)
                    .notificationStatus(NotificationConstants.NotificationStatus.FAILURE)
                    .notificationLogDate(LocalDateTime.now())
                    .build();
            notificationLogRepository.save(smsLog);

            throw new RuntimeException("Failed to send test SMS: " + e.getMessage(), e);
        }
    }

    public void processWithRetry(String timeLabel) {
        log.info("Scheduled SMS processing triggered at {}", timeLabel);

        try {
            ProcessingResult result = processPendingSmsNotifications();

            if (result == ProcessingResult.COB_NOT_FINISHED) {
                log.info("Skipped: COB processing not yet complete");
                return;
            }

            log.info("Scheduled run outcome: {}", result.getDescription());

        } catch (Exception e) {
            log.error("Scheduled processing error at {}: {}", timeLabel, e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldQueueRecords() {
        try {
            LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
            int deletedCount = notificationQueueRepository.deleteSuccessfulRecordsOlderThan(twoDaysAgo);

            if (deletedCount > 0) {
                log.info("Queue cleanup: Deleted {} successful records older than 2 days", deletedCount);
            } else {
                log.debug("Queue cleanup: No old records to delete");
            }

        } catch (Exception e) {
            log.error("Queue cleanup failed: {}", e.getMessage(), e);
        }
    }
}
