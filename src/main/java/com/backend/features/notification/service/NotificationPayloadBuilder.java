package com.backend.features.notification.service;

import com.backend.features.notification.dto.TransmissionFormatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPayloadBuilder {

    private final SignKeyGenerator signKeyGenerator;

    public TransmissionFormatDto buildSmsPayload(String phoneNumber, String messageContent) {
        try {
            String signKey = signKeyGenerator.generateSignKey(phoneNumber, messageContent);
            log.debug("Building SMS payload for phone: {}", phoneNumber);

            return TransmissionFormatDto.builder()
                    .phone(phoneNumber)
                    .content(messageContent)
                    .signKey(signKey)
                    .build();
        } catch (Exception e) {
            log.error("Failed to build SMS payload for phone: {}", phoneNumber, e);
            throw new RuntimeException("Failed to build SMS payload", e);
        }
    }

    public String buildJsonPayload(String phoneNumber, String messageContent) {
        try {
            String signKey = signKeyGenerator.generateSignKey(phoneNumber, messageContent);
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("phone", phoneNumber);
            jsonPayload.put("content", messageContent);
            jsonPayload.put("signKey", signKey);

            log.debug("JSON payload created for phone: {}", phoneNumber);
            return jsonPayload.toString();
        } catch (JSONException e) {
            log.error("Error creating JSON payload for phone: {}", phoneNumber, e);
            throw new RuntimeException("Failed to create JSON payload", e);
        }
    }
}
