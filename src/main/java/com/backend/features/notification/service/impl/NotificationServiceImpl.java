package com.backend.features.notification.service.impl;

import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.models.SmsLog;
import com.backend.features.notification.repository.SmsLogRepository;
import com.backend.features.notification.service.NotificationPayloadBuilder;
import com.backend.features.notification.service.NotificationService;
import com.backend.features.notification.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final RestTemplate restTemplate;
    private final NotificationPayloadBuilder payloadBuilder;
    private final SettingService settingService;
    private final SmsLogRepository smsLogRepository;

    @Value("${cpb.api.url}")
    private String apiUrl;

    @Override
    public void sendNotificationsToMobile() {
        List<SmsLog> logs = smsLogRepository.findAllByStatusNotEqual("SVC-SUCCESS-00");

        logs.forEach(log -> {
            try {
                sendNotification(log.getPhoneNumber(), log);
            } catch (Exception e) {
                log.error("Error processing notification for phone: {}", log.getPhoneNumber(), e);
            }
        });
    }

    private void sendNotification(String phone, SmsLog smsLog) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String content = settingService.getSettingDescription();

        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phone, content);
            log.info("Sending notification payload to phone: {}", phone);

            ResponseEntity<ReceptionFormatDto> response = restTemplate.postForEntity(
                    apiUrl + "/SendOTT",
                    jsonPayload,
                    ReceptionFormatDto.class
            );

            log.info("Received response for phone {}: {}", phone, response.getBody());

            if (response.getBody() != null) {
                updateSmsLogStatus(smsLog, response.getBody().getDesc());
                log.info("Notification sent successfully to phone: {}", phone);
            }
        } catch (RestClientException e) {
            log.error("Failed to send notification to phone {}: {}", phone, e.getMessage());
            updateSmsLogStatus(smsLog, "FAILED");
        }
    }

    private void updateSmsLogStatus(SmsLog smsLog, String status) {
        smsLog.setSmsStatus(status);
        smsLog.setSmsLogDate(LocalDateTime.now());
        smsLogRepository.save(smsLog);
    }
}
