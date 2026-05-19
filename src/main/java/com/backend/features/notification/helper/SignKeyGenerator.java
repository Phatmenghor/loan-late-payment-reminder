package com.backend.features.notification.helper;

import com.backend.config.CpbApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignKeyGenerator {

    private final CpbApiConfig cpbApiConfig;

    public String generateSignKey(String phone, String content) {
        try {
            String encryptionKey = cpbApiConfig.getEncryptionKey();
            String signKeyInput = encryptionKey + phone + encryptionKey + content + encryptionKey;
            return hashWithSha256(signKeyInput);
        } catch (Exception e) {
            log.error("Failed to generate sign key for phone: {}", phone, e);
            return null;
        }
    }

    private String hashWithSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hashBytes) {
                result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
