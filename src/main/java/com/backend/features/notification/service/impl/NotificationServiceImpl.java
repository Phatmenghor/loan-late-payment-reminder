package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
import com.backend.features.notification.dto.request.SendSmsRequest;
import com.backend.features.notification.dto.response.SendSmsResponse;
import com.backend.features.notification.dto.response.SmsLogResponse;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.mapper.SmsLogMapper;
import com.backend.features.notification.models.SmsLog;
import com.backend.features.notification.repository.SmsLogRepository;
import com.backend.features.notification.service.NotificationLogger;
import com.backend.features.notification.service.NotificationPayloadBuilder;
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

    @Override
    public void processPendingSmsNotifications() {
        notificationLogger.logProcessStart();

        try {
            // Step 1: SELECT all pending SMS logs from database
            List<SmsLog> pendingSmsList = selectPendingSmsList();

            if (pendingSmsList.isEmpty()) {
                notificationLogger.logNoPendingSms();
                notificationLogger.logProcessEnd(0, 0);
                return;
            }

            notificationLogger.logPendingSmsFetched(pendingSmsList.size());

            // Step 2: Loop through each pending SMS
            int successCount = 0;
            int failureCount = 0;

            for (SmsLog smsLog : pendingSmsList) {
                try {
                    notificationLogger.logSmsProcessStart(smsLog.getPhoneNumber());

                    // Step 3: Send SMS to API
                    String apiResponse = sendSmsToApi(smsLog.getPhoneNumber(), smsLog.getMessageContent());

                    // Step 4: UPDATE SMS status in database
                    updateSmsLogStatus(smsLog, apiResponse);
                    successCount++;
                    notificationLogger.logSmsSuccess(smsLog.getPhoneNumber(), apiResponse);
                    notificationLogger.logSmsProcessEnd(smsLog.getPhoneNumber());

                } catch (Exception e) {
                    failureCount++;
                    notificationLogger.logSmsFailure(smsLog.getPhoneNumber(), e.getMessage());
                    // UPDATE SMS status as FAILED in database
                    updateSmsLogStatus(smsLog, SMS_STATUS_FAILED);
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
            // Step 1: Create and INSERT SMS log into database
            SmsLog smsLog = createAndInsertSmsLog(request);
            log.info("✓ SMS log created in database with status: {}", smsLog.getSmsStatus());

            // Step 2: Send SMS to API
            String apiResponse = sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());

            // Step 3: UPDATE SMS status in database based on API response
            updateSmsLogStatus(smsLog, apiResponse);
            log.info("✓ SMS status updated in database: {}", apiResponse);

            log.info("========== END: SMS sent successfully ==========");

            return SendSmsResponse.builder()
                    .status("SUCCESS")
                    .message("SMS sent and logged to database")
                    .phoneNumber(request.getPhoneNumber())
                    .smsStatus(apiResponse)
                    .build();

        } catch (Exception e) {
            log.error("✗ Failed to send SMS to phone: {} | Error: {}",
                    request.getPhoneNumber(), e.getMessage());
            log.error("========== END: SMS sending failed ==========");
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

    // ===== PRIVATE METHODS (Following old pattern: SELECT, SEND, UPDATE) =====

    /**
     * Step 1: SELECT all pending SMS logs from database
     * Pattern: SELECT * FROM loan_sms_log WHERE sms_status != 'SVC-SUCCESS-00'
     */
    private List<SmsLog> selectPendingSmsList() {
        log.debug("DATABASE: Executing SELECT for pending SMS notifications");
        return smsLogRepository.findAllByStatusNotEqual(SMS_STATUS_SUCCESS);
    }

    /**
     * Step 2: CREATE and INSERT new SMS log to database
     * Pattern: INSERT INTO loan_sms_log (phone_number, message_content, sms_status, sms_log_date)
     */
    private SmsLog createAndInsertSmsLog(SendSmsRequest request) {
        log.debug("DATABASE: Executing INSERT for new SMS log");
        SmsLog smsLog = SmsLog.builder()
                .phoneNumber(request.getPhoneNumber())
                .messageContent(request.getMessageContent())
                .smsStatus(SMS_STATUS_PENDING)
                .smsLogDate(LocalDateTime.now())
                .build();

        return smsLogRepository.save(smsLog);
    }

    /**
     * Step 3: SEND SMS to external API
     * Returns the status from API response
     */
    private String sendSmsToApi(String phoneNumber, String messageContent) throws RestClientException {
        try {
            // Build JSON payload
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            log.debug("API: Preparing SMS payload for phone: {}", phoneNumber);

            // Get API endpoint from configuration
            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";
            log.info("API: Sending request to: {}", apiUrl);

            // POST request to API
            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    jsonPayload,
                    ReceptionFormatDto.class
            );

            // Extract and return response status
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
     * Step 4: UPDATE SMS status in database based on API response
     * Pattern: UPDATE loan_sms_log SET sms_status = ?, sms_log_date = ? WHERE id = ?
     */
    private void updateSmsLogStatus(SmsLog smsLog, String status) {
        try {
            log.debug("DATABASE: Executing UPDATE for SMS log | Phone: {} | New Status: {}",
                    smsLog.getPhoneNumber(), status);

            smsLog.setSmsStatus(status);
            smsLog.setSmsLogDate(LocalDateTime.now());
            smsLogRepository.save(smsLog);

            log.info("DATABASE: ✓ SMS status updated successfully | Phone: {} | Status: {}",
                    smsLog.getPhoneNumber(), status);

        } catch (Exception e) {
            log.error("DATABASE: ✗ Failed to update SMS status for phone: {}",
                    smsLog.getPhoneNumber(), e);
            throw new RuntimeException("Failed to update SMS log status", e);
        }
    }
}
