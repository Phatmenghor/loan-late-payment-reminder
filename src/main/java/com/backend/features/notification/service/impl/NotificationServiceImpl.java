package com.backend.features.notification.service.impl;

import com.backend.features.notification.dto.request.SendSmsRequest;
import com.backend.features.notification.dto.response.SendSmsResponse;
import com.backend.features.notification.dto.response.SmsLogResponse;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.mapper.SmsLogMapper;
import com.backend.features.notification.models.SmsLog;
import com.backend.features.notification.repository.SmsLogRepository;
import com.backend.features.notification.service.NotificationPayloadBuilder;
import com.backend.features.notification.service.NotificationService;
import com.backend.features.notification.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${cpb.api.url:http://localhost:8080}")
    private String apiUrl;

    @Override
    public void processPendingSmsNotifications() {
        log.info("Processing pending SMS notifications");
        try {
            List<SmsLog> pendingLogs = smsLogRepository.findAllByStatusNotEqual(SMS_STATUS_SUCCESS);

            if (pendingLogs.isEmpty()) {
                log.info("No pending SMS notifications to process");
                return;
            }

            log.info("Found {} pending SMS notifications", pendingLogs.size());
            pendingLogs.forEach(this::processSmsLog);

            log.info("Pending SMS notifications processed successfully");
        } catch (Exception e) {
            log.error("Error processing pending SMS notifications", e);
        }
    }

    @Override
    public SendSmsResponse sendSms(SendSmsRequest request) {
        log.info("Sending SMS to phone: {}", request.getPhoneNumber());

        try {
            SmsLog smsLog = createSmsLog(request);
            SendSmsResponse response = sendSmsToApi(request.getPhoneNumber(), request.getMessageContent());
            smsLog.setSmsStatus(response.getSmsStatus());
            smsLog.setSmsLogDate(LocalDateTime.now());
            smsLogRepository.save(smsLog);

            log.info("SMS sent successfully to phone: {}", request.getPhoneNumber());
            return response;
        } catch (Exception e) {
            log.error("Failed to send SMS to phone: {}", request.getPhoneNumber(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmsLogResponse> getSmsLogs(String phoneNumber) {
        log.debug("Fetching SMS logs for phone: {}", phoneNumber);
        return smsLogRepository.findByPhoneNumber(phoneNumber).stream()
                .map(smsLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmsLogResponse> getAllSmsLogs() {
        log.debug("Fetching all SMS logs");
        return smsLogRepository.findAll().stream()
                .map(smsLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void processSmsLog(SmsLog smsLog) {
        try {
            log.debug("Processing SMS log for phone: {}", smsLog.getPhoneNumber());
            SendSmsResponse response = sendSmsToApi(smsLog.getPhoneNumber(), smsLog.getMessageContent());
            updateSmsLogStatus(smsLog, response.getSmsStatus());
        } catch (Exception e) {
            log.error("Error processing SMS log for phone: {}", smsLog.getPhoneNumber(), e);
            updateSmsLogStatus(smsLog, SMS_STATUS_FAILED);
        }
    }

    private SendSmsResponse sendSmsToApi(String phoneNumber, String messageContent) {
        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            log.debug("Sending SMS payload to API for phone: {}", phoneNumber);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl + "/SendOTT",
                    jsonPayload,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getBody() != null) {
                log.info("API response for phone {}: {}", phoneNumber, apiResponse.getBody().getDesc());
                return SendSmsResponse.builder()
                        .status("SUCCESS")
                        .message("SMS sent successfully")
                        .phoneNumber(phoneNumber)
                        .smsStatus(apiResponse.getBody().getDesc())
                        .build();
            }

            log.error("No response body from API for phone: {}", phoneNumber);
            return createFailedResponse(phoneNumber);
        } catch (RestClientException e) {
            log.error("API error while sending SMS to phone: {}", phoneNumber, e);
            return createFailedResponse(phoneNumber);
        }
    }

    private SendSmsResponse createFailedResponse(String phoneNumber) {
        return SendSmsResponse.builder()
                .status("FAILED")
                .message("Failed to send SMS")
                .phoneNumber(phoneNumber)
                .smsStatus(SMS_STATUS_FAILED)
                .build();
    }

    private SmsLog createSmsLog(SendSmsRequest request) {
        return SmsLog.builder()
                .phoneNumber(request.getPhoneNumber())
                .messageContent(request.getMessageContent())
                .smsStatus(SMS_STATUS_PENDING)
                .smsLogDate(LocalDateTime.now())
                .build();
    }

    private void updateSmsLogStatus(SmsLog smsLog, String status) {
        smsLog.setSmsStatus(status);
        smsLog.setSmsLogDate(LocalDateTime.now());
        smsLogRepository.save(smsLog);
    }
}
