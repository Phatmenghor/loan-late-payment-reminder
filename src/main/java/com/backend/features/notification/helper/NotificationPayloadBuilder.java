package com.backend.features.notification.helper;

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

    public String buildJsonPayload(String phoneNumber, String messageContent) {
        try {
            String signKey = signKeyGenerator.generateSignKey(phoneNumber, messageContent);
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("phone", phoneNumber);
            jsonPayload.put("content", messageContent);
            jsonPayload.put("signKey", signKey);

            return jsonPayload.toString();
        } catch (JSONException e) {
            log.error("Payload: Error creating JSON payload for phone: {}", phoneNumber, e);
            throw new RuntimeException("Failed to create JSON payload", e);
        }
    }
}
