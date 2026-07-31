package com.backend.features.auth.util;

import com.backend.features.auth.dto.AdApiKeyResponse;
import com.backend.features.auth.model.AdApiKey;

import java.security.SecureRandom;
import java.util.Base64;

public final class AdApiKeyUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AdApiKeyUtil() {}

    public static String generateKey(String label) {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String cleanPrefix = (label != null ? label : "app").toLowerCase().replaceAll("[^a-z0-9]", "");
        if (cleanPrefix.isBlank()) {
            cleanPrefix = "app";
        }
        return "ad_sk_" + cleanPrefix + "_" + randomPart;
    }

    public static AdApiKeyResponse toResponse(AdApiKey apiKey) {
        if (apiKey == null) {
            return null;
        }
        return AdApiKeyResponse.builder()
                .id(apiKey.getId())
                .apiKey(apiKey.getApiKey())
                .label(apiKey.getLabel())
                .status(apiKey.getStatus())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .build();
    }
}
