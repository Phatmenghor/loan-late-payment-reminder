package com.backend.features.auth.controller;

import com.backend.features.auth.dto.request.AdApiKeyCreateRequest;
import com.backend.features.auth.dto.request.AdApiKeyStatusUpdateRequest;
import com.backend.features.auth.dto.request.AdConfigUpdateRequest;
import com.backend.features.auth.dto.response.AdApiKeyResponse;
import com.backend.features.auth.dto.response.AdConfigResponse;
import com.backend.features.auth.service.AdApiKeyService;
import com.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/api-keys")
@RequiredArgsConstructor
@Slf4j
public class AdApiKeyController {

    private final AdApiKeyService adApiKeyService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdApiKeyResponse>> createApiKey(@Valid @RequestBody AdApiKeyCreateRequest request) {
        log.info("REST request to generate AD API Key for label: {}", request.getLabel());
        AdApiKeyResponse response = adApiKeyService.createApiKey(request);
        return ResponseEntity.ok(ApiResponse.success("AD API Key generated successfully", response));
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<AdApiKeyResponse>>> getAllApiKeys() {
        log.info("REST request to list all AD API Keys");
        List<AdApiKeyResponse> responses = adApiKeyService.getAllApiKeys();
        return ResponseEntity.ok(ApiResponse.success("AD API Keys retrieved successfully", responses));
    }

    @PostMapping("/ad-config")
    public ResponseEntity<ApiResponse<AdConfigResponse>> getAdConfig() {
        log.info("REST request to get AD configuration");
        AdConfigResponse response = adApiKeyService.getAdConfig();
        return ResponseEntity.ok(ApiResponse.success("AD configuration retrieved successfully", response));
    }

    @PostMapping("/ad-config/update")
    public ResponseEntity<ApiResponse<AdConfigResponse>> updateAdConfig(@Valid @RequestBody AdConfigUpdateRequest request) {
        log.info("REST request to update AD configuration: adEnabled = {}", request.getAdEnabled());
        AdConfigResponse response = adApiKeyService.updateAdConfig(request);
        return ResponseEntity.ok(ApiResponse.success("AD configuration updated successfully", response));
    }

    @PostMapping("/{id}")
    public ResponseEntity<ApiResponse<AdApiKeyResponse>> getApiKeyById(@PathVariable UUID id) {
        log.info("REST request to get AD API Key by ID: {}", id);
        AdApiKeyResponse response = adApiKeyService.getApiKeyById(id);
        return ResponseEntity.ok(ApiResponse.success("AD API Key retrieved successfully", response));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdApiKeyResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdApiKeyStatusUpdateRequest request) {
        log.info("REST request to update status of AD API Key ID: {} to {}", id, request.getStatus());
        AdApiKeyResponse response = adApiKeyService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("AD API Key status updated successfully", response));
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(@PathVariable UUID id) {
        log.info("REST request to delete AD API Key ID: {}", id);
        adApiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.success("AD API Key deleted successfully", null));
    }
}
