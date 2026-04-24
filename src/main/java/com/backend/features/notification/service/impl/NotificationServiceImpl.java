package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
import com.backend.features.notification.config.SmsAsyncConfig;
import com.backend.features.notification.dto.LoanLateReminderDto;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.dto.SendSmsRequestDto;
import com.backend.features.notification.enums.ProcessingResult;
import com.backend.features.notification.helper.CpbHelper;
import com.backend.features.notification.helper.NotificationPayloadBuilder;
import com.backend.features.notification.helper.OracleHelper;
import com.backend.features.notification.models.ProcessingStatus;
import com.backend.features.notification.models.SmsPendingQueue;
import com.backend.features.notification.models.SmsLog;
import com.backend.features.notification.repository.ProcessingStatusRepository;
import com.backend.features.notification.repository.SmsPendingQueueRepository;
import com.backend.features.notification.repository.SmsLogRepository;
import com.backend.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static final String SMS_STATUS_SUCCESS = "SUCCESS";
    private static final String SMS_STATUS_FAILURE = "FAILURE";
    private static final String QUEUE_STATUS_PENDING = "PENDING";
    private static final String QUEUE_STATUS_SUCCESS = "SUCCESS";
    private static final String QUEUE_STATUS_FAILURE = "FAILURE";

    private final RestTemplate restTemplate;
    private final NotificationPayloadBuilder payloadBuilder;
    private final CpbApiConfig cpbApiConfig;
    private final OracleHelper oracleHelper;
    private final SmsLogRepository smsLogRepository;
    private final ProcessingStatusRepository processingStatusRepository;
    private final SmsPendingQueueRepository smsPendingQueueRepository;
    private final CpbHelper cpbHelper;
    private final SmsAsyncConfig smsAsyncConfig;

    @Override
    public ProcessingResult processPendingSmsNotifications() {
        log.info("START: Processing SMS notifications");

        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);
            log.info("Processing for report date: {}", reportDate);

            Optional<ProcessingStatus> existingStatus = processingStatusRepository.findCompleteByReportDate(reportDate);
            if (existingStatus.isPresent()) {
                ProcessingStatus status = existingStatus.get();
                log.info("OPTIMIZATION HIT: Already complete for {}", reportDate);
                log.info("Details - Total: {} | Success: {} | Failure: {} | Completed: {}",
                        status.getTotalCustomers(), status.getSuccessCount(), status.getFailureCount(), status.getCompletedAt());
                log.info("END: Processing SMS notifications");
                return ProcessingResult.ALL_SUCCESS;
            }

            List<SmsPendingQueue> existingQueue = smsPendingQueueRepository.findByReportDate(reportDate);
            if (existingQueue.isEmpty()) {
                log.info("No records in queue for {}, checking Oracle View", reportDate);

                String messageContent = cpbHelper.getContentDescription();
                log.info("Querying View: SELECT * FROM VIEW_LOAN_LATE_REMINDER");
                List<LoanLateReminderDto> records = oracleHelper.selectLoanLateReminderRecords();

                if (records.isEmpty()) {
                    log.info("View is empty - COB might not be finished yet");
                    log.info("END: Processing SMS notifications");
                    return ProcessingResult.COB_NOT_FINISHED;
                }

                log.info("COB finished! Found {} records to store in queue", records.size());
                storeRecordsToQueue(records, messageContent, reportDate);
            } else {
                log.info("Queue already has {} records for {}, skipping view query", existingQueue.size(), reportDate);
            }

            log.info("Batch size: {} | Max threads: {}", smsAsyncConfig.getBatchSize(), smsAsyncConfig.getMaxThreads());

            int[] counts = processQueuedRecords(reportDate);
            int successCount = counts[0];
            int failureCount = counts[1];

            log.info("Processing result - Total success: {} | Total failure: {}", successCount, failureCount);

            checkAndMarkCompletion(reportDate);

            if (failureCount == 0) {
                log.info("All SMS sent successfully");
                log.info("END: Processing SMS notifications");
                return ProcessingResult.ALL_SUCCESS;
            } else {
                log.info("Found {} failures - will retry next hour", failureCount);
                log.info("END: Processing SMS notifications");
                return ProcessingResult.WITH_FAILURES;
            }

        } catch (Exception e) {
            log.error("ERROR in processPendingSmsNotifications: {}", e.getMessage(), e);
            log.info("END: Processing SMS notifications");
            return ProcessingResult.WITH_FAILURES;
        }
    }

    private void storeRecordsToQueue(List<LoanLateReminderDto> records, String messageContent, LocalDate reportDate) {
        try {
            log.info("Storing {} records to queue", records.size());

            for (LoanLateReminderDto record : records) {
                SmsPendingQueue queueRecord = SmsPendingQueue.builder()
                        .customerId(record.getCustomerId())
                        .phoneNumber(record.getPhoneNumber())
                        .arrangementId(record.getArrangementId())
                        .dayDue(record.getDayDue())
                        .reportDate(reportDate)
                        .messageContent(messageContent)
                        .status(QUEUE_STATUS_PENDING)
                        .createdAt(LocalDateTime.now())
                        .retryCount(0)
                        .build();

                smsPendingQueueRepository.save(queueRecord);
            }

            log.info("Stored {} records to queue", records.size());
        } catch (Exception e) {
            log.error("ERROR storing records to queue: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store records to queue", e);
        }
    }

    private int[] processQueuedRecords(LocalDate reportDate) {
        try {
            List<SmsPendingQueue> pendingRecords = smsPendingQueueRepository
                    .findByStatusAndReportDate(QUEUE_STATUS_PENDING, reportDate);

            if (pendingRecords.isEmpty()) {
                log.info("No pending records to process for {}", reportDate);
                return new int[]{0, 0};
            }

            log.info("Found {} pending records to process", pendingRecords.size());

            String messageContent = pendingRecords.get(0).getMessageContent();
            if (messageContent == null) {
                messageContent = cpbHelper.getContentDescription();
            }

            int[] counts = processBatchAsync(pendingRecords, messageContent);
            log.info("Batch processing complete - Success: {} | Failure: {}", counts[0], counts[1]);

            return counts;
        } catch (Exception e) {
            log.error("ERROR processing queued records: {}", e.getMessage(), e);
            return new int[]{0, 0};
        }
    }

    private int[] processBatchAsync(List<SmsPendingQueue> records, String messageContent) {
        int batchSize = smsAsyncConfig.getBatchSize();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        log.info("Starting async batch processing - Total records: {} | Batch size: {}", records.size(), batchSize);

        for (int i = 0; i < records.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, records.size());
            List<SmsPendingQueue> batch = records.subList(i, endIndex);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                processSingleBatch(batch, messageContent, successCount, failureCount);
            });

            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();

        log.info("All batches processed successfully");
        return new int[]{successCount.get(), failureCount.get()};
    }

    private void processSingleBatch(List<SmsPendingQueue> batch, String messageContent, AtomicInteger successCount, AtomicInteger failureCount) {
        for (SmsPendingQueue queueRecord : batch) {
            try {
                Optional<SmsPendingQueue> existingSuccess = smsPendingQueueRepository
                        .findByCustomerIdAndPhoneAndDateAndSuccess(
                                queueRecord.getCustomerId(),
                                queueRecord.getPhoneNumber(),
                                queueRecord.getReportDate());

                if (existingSuccess.isPresent()) {
                    continue;
                }

                sendSmsToApi(queueRecord.getPhoneNumber(), messageContent);
                updateQueueStatus(queueRecord, QUEUE_STATUS_SUCCESS, null);
                successCount.incrementAndGet();
                logToPostgresSQL(queueRecord, SMS_STATUS_SUCCESS, messageContent);

            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.error("SMS sending failed for phone: {}: {}", queueRecord.getPhoneNumber(), e.getMessage());
                updateQueueStatus(queueRecord, QUEUE_STATUS_FAILURE, e.getMessage());
                logToPostgresSQL(queueRecord, SMS_STATUS_FAILURE, messageContent);
            }
        }
    }

    private void updateQueueStatus(SmsPendingQueue queueRecord, String status, String failureReason) {
        try {
            queueRecord.setStatus(status);
            queueRecord.setProcessedAt(LocalDateTime.now());

            if (failureReason != null) {
                queueRecord.setFailureReason(failureReason);
            }

            if (status.equals(QUEUE_STATUS_FAILURE)) {
                queueRecord.setRetryCount(queueRecord.getRetryCount() + 1);
            }

            smsPendingQueueRepository.save(queueRecord);

        } catch (Exception e) {
            log.error("ERROR updating queue status for phone {}: {}", queueRecord.getPhoneNumber(), e.getMessage(), e);
        }
    }

    private void checkAndMarkCompletion(LocalDate reportDate) {
        try {
            long failureCount = smsPendingQueueRepository.countFailureByReportDate(reportDate);
            List<SmsPendingQueue> allRecords = smsPendingQueueRepository.findByReportDate(reportDate);
            long totalCount = allRecords.size();
            long successCount = totalCount - failureCount;

            if (failureCount == 0 && totalCount > 0) {
                Optional<ProcessingStatus> existing = processingStatusRepository.findByReportDate(reportDate);

                ProcessingStatus status = existing.orElseGet(() -> ProcessingStatus.builder()
                        .reportDate(reportDate)
                        .build());

                status.setTotalCustomers((int) totalCount);
                status.setSuccessCount((int) successCount);
                status.setFailureCount((int) failureCount);
                status.setIsComplete(true);
                status.setCompletedAt(LocalDateTime.now());
                status.setLastCheckedAt(LocalDateTime.now());
                status.setNotes("All SMS processed successfully from queue");

                processingStatusRepository.save(status);
                log.info("Marked as complete - Total: {} | Success: {} | Failure: {}",
                        totalCount, successCount, failureCount);
            }

        } catch (Exception e) {
            log.error("ERROR checking completion status: {}", e.getMessage(), e);
        }
    }

    private String sendSmsToApi(String phoneNumber, String messageContent) throws RestClientException {
        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getBody() != null && apiResponse.getBody().getDesc() != null) {
                return apiResponse.getBody().getDesc();
            }

            log.warn("No response body received from CPB API");
            return SMS_STATUS_FAILURE;

        } catch (RestClientException e) {
            log.error("CPB API request failed for phone: {}: {}", phoneNumber, e.getMessage(), e);
            throw e;
        }
    }

    private void logToPostgresSQL(SmsPendingQueue queueRecord, String status, String messageContent) {
        try {
            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(queueRecord.getPhoneNumber())
                    .customerId(queueRecord.getCustomerId())
                    .reportDate(queueRecord.getReportDate())
                    .arrangementId(queueRecord.getArrangementId())
                    .dayDue(queueRecord.getDayDue())
                    .messageContent(messageContent)
                    .smsStatus(status)
                    .smsLogDate(LocalDateTime.now())
                    .build();

            smsLogRepository.save(smsLog);

        } catch (Exception e) {
            log.error("ERROR saving audit log for phone: {}: {}", queueRecord.getPhoneNumber(), e.getMessage(), e);
        }
    }

    @Override
    public String sendTestSms(SendSmsRequestDto request) {
        log.info("TEST SMS: Sending to phone: {}", request.getPhoneNumber());

        try {
            sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());

            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .messageContent(request.getMessageContent())
                    .smsStatus(SMS_STATUS_SUCCESS)
                    .smsLogDate(LocalDateTime.now())
                    .build();
            smsLogRepository.save(smsLog);

            log.info("TEST SMS: Sent successfully");
            return SMS_STATUS_SUCCESS;

        } catch (Exception e) {
            log.error("TEST SMS: Failed for phone: {}: {}", request.getPhoneNumber(), e.getMessage(), e);

            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .messageContent(request.getMessageContent())
                    .smsStatus(SMS_STATUS_FAILURE)
                    .smsLogDate(LocalDateTime.now())
                    .build();
            smsLogRepository.save(smsLog);

            throw new RuntimeException("Failed to send test SMS: " + e.getMessage(), e);
        }
    }

    public void processWithRetry(String timeLabel) {
        log.info("START: Hourly SMS processing at {}", timeLabel);

        try {
            ProcessingResult result = processPendingSmsNotifications();

            if (result == ProcessingResult.COB_NOT_FINISHED) {
                log.info("RESULT: COB not finished - skipping processing");
                return;
            }

            log.info("RESULT: {} - processing complete or will retry", result.getDescription());

        } catch (Exception e) {
            log.error("ERROR in processWithRetry at {}: {}", timeLabel, e.getMessage(), e);
        }

        log.info("END: Hourly SMS processing");
    }
}
