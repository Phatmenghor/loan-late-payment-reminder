package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
import com.backend.features.notification.dto.request.SendSmsRequest;
import com.backend.features.notification.dto.response.SendSmsResponse;
import com.backend.features.notification.dto.response.SmsLogResponse;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.helper.NotificationLogger;
import com.backend.features.notification.helper.NotificationPayloadBuilder;
import com.backend.features.notification.helper.OracleHelper;
import com.backend.features.notification.mapper.SmsLogMapper;
import com.backend.features.notification.models.SmsLog;
import com.backend.features.notification.repository.SmsLogRepository;
import com.backend.features.notification.service.NotificationService;
import com.backend.features.notification.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String SMS_STATUS_SUCCESS = "SVC-SUCCESS-00";
    private static final String SMS_STATUS_FAILED = "SVC-FAILED";
    private static final String SMS_STATUS_PENDING = "PENDING";

    private final RestTemplate restTemplate;
    private final NotificationPayloadBuilder payloadBuilder;
    private final SettingService settingService;
    private final SmsLogRepository smsLogRepository;
    private final SmsLogMapper smsLogMapper;
    private final CpbApiConfig cpbApiConfig;
    private final NotificationLogger notificationLogger;
    private final OracleHelper oracleHelper;

    @Override
    public void processPendingSmsNotifications() {
        notificationLogger.logProcessStart();

        try {
            // Step 1: SELECT all pending SMS phone numbers from Oracle
            List<String> phoneNumbers = oracleHelper.selectPendingSmsPhoneNumbers();

            if (phoneNumbers.isEmpty()) {
                notificationLogger.logNoPendingSms();
                notificationLogger.logProcessEnd(0, 0);
                return;
            }

            notificationLogger.logPendingSmsFetched(phoneNumbers.size());

            // Step 2: Loop through each pending SMS
            int successCount = 0;
            int failureCount = 0;

            for (String phoneNumber : phoneNumbers) {
                try {
                    notificationLogger.logSmsProcessStart(phoneNumber);

                    // Step 3: Send SMS to API
                    String apiResponse = sendSmsToApi(phoneNumber, "Loan payment reminder");

                    // Step 4: UPDATE SMS status in Oracle
                    oracleHelper.updateSmsStatus(phoneNumber, apiResponse);
                    successCount++;

                    // Step 5: LOG the result to PostgreSQL for audit trail
                    logSmsResult(phoneNumber, apiResponse);
                    notificationLogger.logSmsSuccess(phoneNumber, apiResponse);
                    notificationLogger.logSmsProcessEnd(phoneNumber);

                } catch (Exception e) {
                    failureCount++;
                    notificationLogger.logSmsFailure(phoneNumber, e.getMessage());
                    // UPDATE SMS status as FAILED in Oracle
                    try {
                        oracleHelper.updateSmsStatus(phoneNumber, SMS_STATUS_FAILED);
                    } catch (Exception ex) {
                        notificationLogger.logException("Oracle UPDATE on failure", ex);
                    }
                    // LOG the failure to PostgreSQL
                    logSmsFailure(phoneNumber, e.getMessage());
                    notificationLogger.logException("SMS Processing", e);
                }
            }

            notificationLogger.logProcessEnd(successCount, failureCount);

        } catch (Exception e) {
            notificationLogger.logException("Pending SMS Processing", e);
        }
    }

    @Override
    public SendSmsResponse sendSms(SendSmsRequest request) {
        log.info("========== START: Sending SMS to phone: {} ==========", request.getPhoneNumber());

        try {
            // Step 1: Send SMS to API
            String apiResponse = sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());
            log.info("✓ API response received: {}", apiResponse);

            // Step 2: LOG result to PostgreSQL for audit trail
            logSmsResult(request.getPhoneNumber(), apiResponse);
            log.info("✓ SMS result logged to PostgreSQL");

            log.info("========== END: SMS sent successfully ==========");

            return SendSmsResponse.builder()
                    .status("SUCCESS")
                    .message("SMS sent and logged")
                    .phoneNumber(request.getPhoneNumber())
                    .smsStatus(apiResponse)
                    .build();

        } catch (Exception e) {
            log.error("✗ Failed to send SMS to phone: {} | Error: {}",
                    request.getPhoneNumber(), e.getMessage());
            log.error("========== END: SMS sending failed ==========");
            // LOG failure to PostgreSQL
            logSmsFailure(request.getPhoneNumber(), e.getMessage());
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmsLogResponse> getSmsLogs(String phoneNumber) {
        log.info("SELECT: Fetching SMS logs from database for phone: {}", phoneNumber);
        return smsLogRepository.findByPhoneNumber(phoneNumber).stream()
                .map(smsLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmsLogResponse> getAllSmsLogs() {
        log.info("SELECT: Fetching all SMS logs from database");
        return smsLogRepository.findAll().stream()
                .map(smsLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== PRIVATE METHODS =====

    /**
     * CREATE and INSERT new SMS log to PostgreSQL (for manual sends)
     */
    private SmsLog createAndInsertSmsLog(SendSmsRequest request) {
        log.debug("PostgreSQL: Executing INSERT for new SMS log");
        SmsLog smsLog = SmsLog.builder()
                .phoneNumber(request.getPhoneNumber())
                .messageContent(request.getMessageContent())
                .smsStatus(SMS_STATUS_PENDING)
                .smsLogDate(LocalDateTime.now())
                .build();

        return smsLogRepository.save(smsLog);
    }

    /**
     * SEND SMS to external API
     * Returns the status from API response
     */
    private String sendSmsToApi(String phoneNumber, String messageContent) throws RestClientException {
        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            log.debug("API: Preparing SMS payload for phone: {}", phoneNumber);

            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";
            log.info("API: Sending request to: {}", apiUrl);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    jsonPayload,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getBody() != null && apiResponse.getBody().getDesc() != null) {
                String responseStatus = apiResponse.getBody().getDesc();
                log.info("API: Received response status: {}", responseStatus);
                return responseStatus;
            }

            log.warn("API: No response body received");
            return SMS_STATUS_FAILED;

        } catch (RestClientException e) {
            log.error("API: Request failed for phone: {}", phoneNumber, e);
            throw e;
        }
    }

    /**
     * LOG SMS result to PostgreSQL (for manual sends from REST API)
     */
    private void logSmsResult(String phoneNumber, String status) {
        try {
            log.debug("PostgreSQL: Logging SMS result for phone: {} | Status: {}", phoneNumber, status);

            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(phoneNumber)
                    .messageContent("Loan payment reminder")
                    .smsStatus(status)
                    .smsLogDate(LocalDateTime.now())
                    .build();

            smsLogRepository.save(smsLog);
            log.info("PostgreSQL: ✓ SMS result logged successfully | Phone: {} | Status: {}", phoneNumber, status);

        } catch (Exception e) {
            log.error("PostgreSQL: ✗ Failed to log SMS result for phone: {}", phoneNumber, e);
        }
    }

    /**
     * LOG SMS failure to PostgreSQL (for audit trail)
     */
    private void logSmsFailure(String phoneNumber, String errorMessage) {
        try {
            log.debug("PostgreSQL: Logging SMS failure for phone: {}", phoneNumber);

            SmsLog smsLog = SmsLog.builder()
                    .phoneNumber(phoneNumber)
                    .messageContent("Loan payment reminder")
                    .smsStatus(SMS_STATUS_FAILED + " - " + errorMessage)
                    .smsLogDate(LocalDateTime.now())
                    .build();

            smsLogRepository.save(smsLog);
            log.info("PostgreSQL: ✓ SMS failure logged successfully | Phone: {}", phoneNumber);

        } catch (Exception e) {
            log.error("PostgreSQL: ✗ Failed to log SMS failure for phone: {}", phoneNumber, e);
        }
    }
}
