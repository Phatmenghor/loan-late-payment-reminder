package com.backend.features.auth.controller;

import com.backend.features.auth.dto.AdApiKeyCreateRequest;
import com.backend.features.auth.dto.AdApiKeyResponse;
import com.backend.features.auth.dto.AdApiKeyStatusUpdateRequest;
import com.backend.features.auth.dto.AdConfigResponse;
import com.backend.features.auth.dto.AdConfigUpdateRequest;
import com.backend.features.auth.service.AdApiKeyService;
import com.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/api-keys")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AD API Key Management", description = "Endpoints for generating, listing, locking/unlocking, revoking AD System API Keys, and configuring Active Directory settings — Protected by Basic Auth")
@SecurityRequirement(name = "basicAuth")
public class AdApiKeyController {

    private final AdApiKeyService adApiKeyService;

    @PostMapping
    @Operation(summary = "Generate a new AD API Key for a system", description = "Creates a new active AD API Key with a unique system label")
    public ResponseEntity<ApiResponse<AdApiKeyResponse>> createApiKey(@Valid @RequestBody AdApiKeyCreateRequest request) {
        log.info("REST request to generate AD API Key for label: {}", request.getLabel());
        AdApiKeyResponse response = adApiKeyService.createApiKey(request);
        return ResponseEntity.ok(ApiResponse.success("AD API Key generated successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all AD API Keys", description = "Retrieves all active non-deleted AD API keys in the database")
    public ResponseEntity<ApiResponse<List<AdApiKeyResponse>>> getAllApiKeys() {
        log.info("REST request to list all AD API Keys");
        List<AdApiKeyResponse> responses = adApiKeyService.getAllApiKeys();
        return ResponseEntity.ok(ApiResponse.success("AD API Keys retrieved successfully", responses));
    }

    @GetMapping("/ad-config")
    @Operation(summary = "Get Active Directory Configuration", description = "Retrieves current AD settings including adEnabled status flag")
    public ResponseEntity<ApiResponse<AdConfigResponse>> getAdConfig() {
        log.info("REST request to get AD configuration");
        AdConfigResponse response = adApiKeyService.getAdConfig();
        return ResponseEntity.ok(ApiResponse.success("AD configuration retrieved successfully", response));
    }

    @PutMapping("/ad-config")
    @Operation(summary = "Update Active Directory Configuration", description = "Updates AD settings (e.g. toggle adEnabled status flag)")
    public ResponseEntity<ApiResponse<AdConfigResponse>> updateAdConfig(@Valid @RequestBody AdConfigUpdateRequest request) {
        log.info("REST request to update AD configuration: adEnabled = {}", request.getAdEnabled());
        AdConfigResponse response = adApiKeyService.updateAdConfig(request);
        return ResponseEntity.ok(ApiResponse.success("AD configuration updated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get AD API Key details by ID")
    public ResponseEntity<ApiResponse<AdApiKeyResponse>> getApiKeyById(@PathVariable UUID id) {
        log.info("REST request to get AD API Key by ID: {}", id);
        AdApiKeyResponse response = adApiKeyService.getApiKeyById(id);
        return ResponseEntity.ok(ApiResponse.success("AD API Key retrieved successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Lock / Unlock an AD API Key", description = "Updates status to ACTIVE or INACTIVE to lock or unlock access")
    public ResponseEntity<ApiResponse<AdApiKeyResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdApiKeyStatusUpdateRequest request) {
        log.info("REST request to update status of AD API Key ID: {} to {}", id, request.getStatus());
        AdApiKeyResponse response = adApiKeyService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("AD API Key status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete / Revoke an AD API Key")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(@PathVariable UUID id) {
        log.info("REST request to delete AD API Key ID: {}", id);
        adApiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.success("AD API Key deleted successfully", null));
    }
}
