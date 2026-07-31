package com.backend.features.auth.service;

import com.backend.enums.common.Status;
import com.backend.features.auth.dto.AdApiKeyCreateRequest;
import com.backend.features.auth.dto.AdApiKeyResponse;
import com.backend.features.auth.dto.AdConfigResponse;
import com.backend.features.auth.dto.AdConfigUpdateRequest;
import com.backend.features.auth.model.AdApiKey;

import java.util.List;
import java.util.UUID;

public interface AdApiKeyService {

    AdApiKeyResponse createApiKey(AdApiKeyCreateRequest request);

    List<AdApiKeyResponse> getAllApiKeys();

    AdApiKeyResponse getApiKeyById(UUID id);

    AdApiKeyResponse updateStatus(UUID id, Status status);

    void deleteApiKey(UUID id);

    AdApiKey validateApiKey(String apiKey);

    AdConfigResponse getAdConfig();

    AdConfigResponse updateAdConfig(AdConfigUpdateRequest request);
}
