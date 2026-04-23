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

    public TransmissionFormatDto buildPayload(String phone, String content) throws Exception {
        String signKey = signKeyGenerator.getSignKey(phone, content);
        return TransmissionFormatDto.builder()
                .phone(phone)
                .content(content)
                .signKey(signKey)
                .build();
    }

    public String buildJsonPayload(String phone, String content) throws Exception {
        JSONObject jsonObject = new JSONObject();
        String signKey = signKeyGenerator.getSignKey(phone, content);
        try {
            jsonObject.put("phone", phone);
            jsonObject.put("content", content);
            jsonObject.put("signKey", signKey);
        } catch (JSONException e) {
            log.error("Error creating JSON payload", e);
            throw new RuntimeException("Failed to create JSON payload", e);
        }
        return jsonObject.toString();
    }
}
