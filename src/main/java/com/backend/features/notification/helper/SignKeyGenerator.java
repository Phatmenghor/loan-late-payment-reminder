package com.backend.features.notification.helper;

import com.backend.config.CpbApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignKeyGenerator {

    private final RestTemplate restTemplate;
    private final CpbApiConfig cpbApiConfig;

    public String generateSignKey(String phone, String content) {
        try {
            String encryptionKey = cpbApiConfig.getEncryptionKey();
            String signKeyInput = encryptionKey + phone + content;
            String signKey = hashWithSha256(signKeyInput);
            log.info("Sign key generated for phone: {} | input pattern: KEY+PHONE+CONTENT | hash: {}", phone, signKey);
            return signKey;
        } catch (Exception e) {
            log.error("Failed to generate sign key for phone: {}", phone, e);
            return null;
        }
    }

    public String hashWithSha256(String input) {
        return hash(input, "SHA-256");
    }

    public String hashWithMd5(String input) {
        return hash(input, "MD5");
    }

    private String hash(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return convertBytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm + " not available", e);
        }
    }

    public String hmacSha256(String secretKey, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return convertBytesToHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA256 failed", e);
        }
    }

    public String hmacMd5(String secretKey, String message) {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacMD5"));
            return convertBytesToHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HmacMD5 failed", e);
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
