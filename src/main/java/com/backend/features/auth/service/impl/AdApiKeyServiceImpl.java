package com.backend.features.auth.service.impl;

import com.backend.enums.common.Status;
import com.backend.exception.custom.NotFoundException;
import com.backend.exception.custom.ValidationException;
import com.backend.features.auth.dto.AdApiKeyCreateRequest;
import com.backend.features.auth.dto.AdApiKeyResponse;
import com.backend.features.auth.dto.AdConfigResponse;
import com.backend.features.auth.dto.AdConfigUpdateRequest;
import com.backend.features.auth.mapper.AdApiKeyMapper;
import com.backend.features.auth.mapper.AdConfigMapper;
import com.backend.features.auth.model.AdApiKey;
import com.backend.features.auth.model.AdConfig;
import com.backend.features.auth.repository.AdApiKeyRepository;
import com.backend.features.auth.repository.AdConfigRepository;
import com.backend.features.auth.service.AdApiKeyService;
import com.backend.features.auth.util.AdApiKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdApiKeyServiceImpl implements AdApiKeyService {

    private final AdApiKeyRepository adApiKeyRepository;
    private final AdConfigRepository adConfigRepository;
    private final AdApiKeyMapper adApiKeyMapper;
    private final AdConfigMapper adConfigMapper;

    @Override
    @Transactional
    public AdApiKeyResponse createApiKey(AdApiKeyCreateRequest request) {
        String label = request.getLabel();
        log.info("Creating new AD API key for label: {}", label);

        if (adApiKeyRepository.existsByLabelAndIsDeletedFalse(label)) {
            log.warn("AD API Key creation failed: label [{}] already exists", label);
            throw new ValidationException("AD API Key with label '" + label + "' already exists");
        }

        String rawKey = AdApiKeyUtil.generateKey(label);
        AdApiKey apiKey = AdApiKey.builder()
                .apiKey(rawKey)
                .label(label)
                .status(Status.ACTIVE)
                .build();

        AdApiKey savedKey = adApiKeyRepository.save(apiKey);
        log.info("Successfully created AD API key ID: {} for label: {}", savedKey.getId(), savedKey.getLabel());
        return adApiKeyMapper.toResponse(savedKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdApiKeyResponse> getAllApiKeys() {
        log.info("Fetching all active non-deleted AD API keys");
        List<AdApiKey> keys = adApiKeyRepository.findByIsDeletedFalse();
        log.info("Retrieved {} AD API key(s)", keys.size());
        return adApiKeyMapper.toResponseList(keys);
    }

    @Override
    @Transactional(readOnly = true)
    public AdApiKeyResponse getApiKeyById(UUID id) {
        log.info("Fetching AD API key by ID: {}", id);
        AdApiKey apiKey = adApiKeyRepository.findById(id)
                .filter(k -> !Boolean.TRUE.equals(k.getIsDeleted()))
                .orElseThrow(() -> {
                    log.warn("AD API key not found with ID: {}", id);
                    return new NotFoundException("AD API Key not found with ID: " + id);
                });
        return adApiKeyMapper.toResponse(apiKey);
    }

    @Override
    @Transactional
    public AdApiKeyResponse updateStatus(UUID id, Status status) {
        log.info("Updating status for AD API key ID: {} to {}", id, status);
        AdApiKey apiKey = adApiKeyRepository.findById(id)
                .filter(k -> !Boolean.TRUE.equals(k.getIsDeleted()))
                .orElseThrow(() -> {
                    log.warn("AD API key not found with ID: {}", id);
                    return new NotFoundException("AD API Key not found with ID: " + id);
                });

        apiKey.setStatus(status);
        AdApiKey updatedKey = adApiKeyRepository.save(apiKey);
        log.info("Successfully updated AD API key ID: {} status to {}", updatedKey.getId(), updatedKey.getStatus());
        return adApiKeyMapper.toResponse(updatedKey);
    }

    @Override
    @Transactional
    public void deleteApiKey(UUID id) {
        log.info("Deleting (soft-delete) AD API key ID: {}", id);
        AdApiKey apiKey = adApiKeyRepository.findById(id)
                .filter(k -> !Boolean.TRUE.equals(k.getIsDeleted()))
                .orElseThrow(() -> {
                    log.warn("AD API key not found for deletion with ID: {}", id);
                    return new NotFoundException("AD API Key not found with ID: " + id);
                });

        apiKey.softDelete();
        adApiKeyRepository.save(apiKey);
        log.info("Successfully soft-deleted AD API key ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public AdApiKey validateApiKey(String apiKey) {
        log.debug("Validating AD API key: {}", apiKey);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AD API key validation failed: key is null or blank");
            throw new ValidationException("AD API Key is required to access AD login");
        }

        AdApiKey keyRecord = adApiKeyRepository.findByApiKeyAndIsDeletedFalse(apiKey)
                .orElseThrow(() -> {
                    log.warn("AD API key validation failed: key not found");
                    return new ValidationException("Invalid AD API Key");
                });

        if (keyRecord.getStatus() == Status.INACTIVE) {
            log.warn("AD API key validation failed: key ID {} is INACTIVE (locked)", keyRecord.getId());
            throw new ValidationException("AD API Key is inactive or locked");
        }

        log.debug("AD API key validated successfully for system label: {}", keyRecord.getLabel());
        return keyRecord;
    }

    @Override
    @Transactional
    public AdConfigResponse getAdConfig() {
        log.info("Fetching AD configuration");
        AdConfig config = adConfigRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> {
                    log.info("No AD config found - creating default active AD config");
                    AdConfig defaultConfig = AdConfig.builder()
                            .adEnabled(true)
                            .description("Default Active Directory configuration")
                            .build();
                    return adConfigRepository.save(defaultConfig);
                });

        return adConfigMapper.toResponse(config);
    }

    @Override
    @Transactional
    public AdConfigResponse updateAdConfig(AdConfigUpdateRequest request) {
        log.info("Updating AD configuration: adEnabled = {}", request.getAdEnabled());
        AdConfig config = adConfigRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> AdConfig.builder().adEnabled(true).build());

        config.setAdEnabled(request.getAdEnabled());
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }

        AdConfig updatedConfig = adConfigRepository.save(config);
        log.info("Successfully updated AD config ID: {} [adEnabled={}]", updatedConfig.getId(), updatedConfig.getAdEnabled());
        return adConfigMapper.toResponse(updatedConfig);
    }
}
