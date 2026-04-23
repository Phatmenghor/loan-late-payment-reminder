package com.backend.features.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignKeyGenerator {

    private final RestTemplate restTemplate;

    @Value("${cpb.api.url:http://localhost:8080}")
    private String apiUrl;

    public String generateSignKey(String phone, String content) {
        try {
            String payload = createPayload(phone, content);
            log.debug("Generating sign key for phone: {}", phone);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl + "/GenKey",
                    payload,
                    String.class
            );

            String signKey = response.getBody();
            log.info("Sign key generated successfully for phone: {}", phone);
            return signKey;
        } catch (RestClientException e) {
            log.error("Failed to generate sign key from API for phone: {}", phone, e);
            return null;
        }
    }

    public String hashWithSha256(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(input.getBytes());
            byte[] hashBytes = messageDigest.digest();
            return convertBytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String createPayload(String phone, String content) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("phone", phone);
            jsonObject.put("content", content);
            return jsonObject.toString();
        } catch (JSONException e) {
            log.error("Error creating JSON payload", e);
            throw new RuntimeException("Failed to create JSON payload", e);
        }
    }

    private String convertBytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
