package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
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
                NotificationQueue queueRecord = NotificationQueue.builder()
                        .customerId(record.getCustomerId())
                        .phoneNumber(record.getPhoneNumber())
                        .arrangementId(record.getArrangementId())
                        .reportDate(reportDate)
                        .messageContent(messageContent)
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
            List<NotificationQueue> recordsToProcess = notificationQueueRepository
                    .findByStatusAndReportDate(NotificationConstants.QueueStatus.PENDING, reportDate);

            if (recordsToProcess.isEmpty()) {
                log.info("No pending records to process for {}", reportDate);
                return new int[]{0, 0};
            }

            log.info("Starting delivery: {} pending messages in queue", recordsToProcess.size());

            String messageContent = recordsToProcess.get(0).getMessageContent();
            if (messageContent == null) {
                messageContent = cpbHelper.getContentDescription();
            }

            int success = 0;
            int failure = 0;

            for (NotificationQueue queueRecord : recordsToProcess) {
                try {
                    String[] result = sendSmsToApi(queueRecord.getPhoneNumber(), messageContent);
                    String apiStatus = result[0];
                    String apiMessage = result[1];

                    if (NotificationConstants.NotificationStatus.SUCCESS.equals(apiStatus)) {
                        updateQueueStatus(queueRecord, NotificationConstants.QueueStatus.SUCCESS, null);
                        logToPostgresSQL(queueRecord, NotificationConstants.NotificationStatus.SUCCESS, messageContent, null);
                        success++;
                    } else {
                        updateQueueStatus(queueRecord, NotificationConstants.QueueStatus.FAILURE, apiMessage);
                        logToPostgresSQL(queueRecord, NotificationConstants.NotificationStatus.FAILURE, messageContent, apiMessage);
                        failure++;
                    }

                } catch (Exception e) {
                    failure++;
                    log.warn("Delivery failed for {}: {}", queueRecord.getPhoneNumber(), e.getMessage());
                    updateQueueStatus(queueRecord, NotificationConstants.QueueStatus.FAILURE, e.getMessage());
                    logToPostgresSQL(queueRecord, NotificationConstants.NotificationStatus.FAILURE, messageContent, e.getMessage());
                }
            }

            log.info("Delivery complete: {} successful, {} failed", success, failure);
            return new int[]{success, failure};

        } catch (Exception e) {
            log.error("Queue processing aborted: {}", e.getMessage(), e);
            return new int[]{0, 0};
        }
    }

    private void updateQueueStatus(NotificationQueue queueRecord, String status, String failureReason) {
        try {
            notificationQueueRepository.updateQueueStatusById(
                    queueRecord.getId(),
                    status,
                    LocalDateTime.now(),
                    failureReason);
        } catch (Exception e) {
            log.error("Failed to update queue status for {}: {}", queueRecord.getPhoneNumber(), e.getMessage(), e);
        }
    }

    private void checkAndMarkCompletion(LocalDate reportDate) {
        try {
            long failureCount = notificationQueueRepository.countFailureByReportDate(reportDate);
            List<NotificationQueue> allRecords = notificationQueueRepository.findByReportDate(reportDate);
            long totalCount = allRecords.size();
            long successCount = totalCount - failureCount;

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
            status.setNotes("Processed: " + successCount + " success, " + failureCount + " failed");

            notificationProcessingStatusRepository.save(status);
            log.info("Completion recorded: {} success, {} failed", successCount, failureCount);

        } catch (Exception e) {
            log.error("Completion check failed: {}", e.getMessage(), e);
        }
    }

    private String[] sendSmsToApi(String phoneNumber, String messageContent) {
        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            log.info("Notification API call: POST {} | Phone: {}", apiUrl, phoneNumber);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(apiUrl, request, ReceptionFormatDto.class);

            if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null) {
                ReceptionFormatDto body = apiResponse.getBody();
                String code = body.getCode();
                String desc = body.getDesc();

                if (code != null && (code.equals("0") || code.equals("00"))) {
                    log.info("Notification delivered to {}: Code={}, Desc={}", phoneNumber, code, desc);
                    return new String[]{NotificationConstants.NotificationStatus.SUCCESS, null};
                }

                log.warn("Notification failed for {}: Code={}, Desc={}", phoneNumber, code, desc);
                return new String[]{NotificationConstants.NotificationStatus.FAILURE, "Code=" + code + ", Desc=" + desc};
            }

            log.warn("Notification API error for {} | Status: {}", phoneNumber, apiResponse.getStatusCode());
            return new String[]{NotificationConstants.NotificationStatus.FAILURE, "HTTP " + apiResponse.getStatusCode()};

        } catch (RestClientException e) {
            log.error("Notification API call failed for {}: {}", phoneNumber, e.getMessage());
            return new String[]{NotificationConstants.NotificationStatus.FAILURE, e.getMessage()};
        }
    }

    private void logToPostgresSQL(NotificationQueue queueRecord, String status, String messageContent, String failureReason) {
        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(queueRecord.getPhoneNumber(), messageContent);

            NotificationLog smsLog = NotificationLog.builder()
                    .phoneNumber(queueRecord.getPhoneNumber())
                    .customerId(queueRecord.getCustomerId())
                    .reportDate(queueRecord.getReportDate())
                    .arrangementId(queueRecord.getArrangementId())
                    .jsonPayload(jsonPayload)
                    .notificationStatus(status)
                    .failureReason(failureReason)
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
            String[] result = sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());
            String status = result[0];
            String apiMessage = result[1];

            NotificationLog smsLog = NotificationLog.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .jsonPayload(jsonPayload)
                    .notificationStatus(status)
                    .failureReason(apiMessage)
                    .notificationLogDate(LocalDateTime.now())
                    .build();
            notificationLogRepository.save(smsLog);

            log.info("Test SMS result for {}: {}", request.getPhoneNumber(), status);
            return status;

        } catch (Exception e) {
            log.error("Test SMS delivery failed: {}", e.getMessage(), e);

            NotificationLog smsLog = NotificationLog.builder()
                    .phoneNumber(request.getPhoneNumber())
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

}
