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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.profiles.active:}")
    private String activeProfile;

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
        log.info("BEGIN: SMS notification processing cycle");

        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);
            log.info("Target report date: {}", reportDate);

            Optional<ProcessingStatus> existingStatus = processingStatusRepository.findCompleteByReportDate(reportDate);
            if (existingStatus.isPresent()) {
                ProcessingStatus status = existingStatus.get();
                log.info("CACHE HIT: {} already processed successfully", reportDate);
                log.info("Cached results: {} total, {} success, {} failure (completed at {})",
                        status.getTotalCustomers(), status.getSuccessCount(), status.getFailureCount(), status.getCompletedAt());
                return ProcessingResult.ALL_SUCCESS;
            }

            List<SmsPendingQueue> existingQueue = smsPendingQueueRepository.findByReportDate(reportDate);
            if (existingQueue.isEmpty()) {
                log.info("Queue empty for {}, fetching from Oracle View", reportDate);

                String messageContent = cpbHelper.getContentDescription();
                List<LoanLateReminderDto> records = oracleHelper.selectLoanLateReminderRecords();

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

            log.info("Queued: {} records added with status=PENDING", records.size());
        } catch (Exception e) {
            log.error("Queue persistence failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store records to queue", e);
        }
    }

    private int[] processQueuedRecords(LocalDate reportDate) {
        try {
            List<SmsPendingQueue> pendingRecords = smsPendingQueueRepository
                    .findByStatusAndReportDate(QUEUE_STATUS_PENDING, reportDate);

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

    private int[] processBatchAsync(List<SmsPendingQueue> records, String messageContent) {
        int batchSize = smsAsyncConfig.getBatchSize();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();


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
                log.warn("Delivery failed for {}: {}", queueRecord.getPhoneNumber(), e.getMessage());
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
            log.error("Failed to update queue status for {}: {}", queueRecord.getPhoneNumber(), e.getMessage(), e);
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
                log.info("Completion recorded: {} messages processed (all successful)", totalCount);
            } else if (failureCount > 0) {
                log.info("Completion check: {} successful, {} pending retry", successCount, failureCount);
            }

        } catch (Exception e) {
            log.error("Completion check failed: {}", e.getMessage(), e);
        }
    }

    private String sendSmsToApi(String phoneNumber, String messageContent) throws RestClientException {
        if ("local".equals(activeProfile)) {
            log.info("SMS dispatch (LOCAL): {} | Message: {}", phoneNumber, messageContent);
            return SMS_STATUS_SUCCESS;
        }

        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";

            log.info("SMS API call: POST {} | Phone: {} | Content: {}", apiUrl, phoneNumber, messageContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getBody() != null && apiResponse.getBody().getDesc() != null) {
                log.info("SMS API response: {} | Status: {}", phoneNumber, apiResponse.getBody().getDesc());
                return apiResponse.getBody().getDesc();
            }

            log.warn("SMS API empty response: {}", phoneNumber);
            return SMS_STATUS_FAILURE;

        } catch (RestClientException e) {
            log.error("SMS API call failed: {} | Error: {}", phoneNumber, e.getMessage(), e);
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
            log.error("Audit log save failed for {}: {}", queueRecord.getPhoneNumber(), e.getMessage(), e);
        }
    }

    @Override
    public String sendTestSms(SendSmsRequestDto request) {
        log.info("Test endpoint invoked for {}", request.getPhoneNumber());

        try {
            sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());

            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .messageContent(request.getMessageContent())
                    .smsStatus(SMS_STATUS_SUCCESS)
                    .smsLogDate(LocalDateTime.now())
                    .build();
            smsLogRepository.save(smsLog);

            log.info("Test SMS delivered successfully");
            return SMS_STATUS_SUCCESS;

        } catch (Exception e) {
            log.error("Test SMS delivery failed: {}", e.getMessage(), e);

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
}
