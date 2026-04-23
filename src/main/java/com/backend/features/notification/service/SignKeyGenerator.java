package com.backend.features.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.json.JSONObject;
import org.json.JSONException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@Slf4j
public class SignKeyGenerator {

    @Value("${cpb.api.url}")
    private String apiUrl;

    @Autowired
    private RestTemplate restTemplate;

    public String getSignKey(String phone, String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String json = createSignKeyPayload(phone, content);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl + "/GenKey", json, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to generate sign key from API", e);
            return null;
        }
    }

    private String createSignKeyPayload(String phone, String content) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("phone", phone);
            jsonObject.put("content", content);
        } catch (JSONException e) {
            log.error("Error creating JSON payload for sign key", e);
        }
        return jsonObject.toString();
    }

    public String getSha256(String base) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(base.getBytes());
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 algorithm not available", ex);
            throw new RuntimeException("SHA-256 algorithm not available", ex);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
