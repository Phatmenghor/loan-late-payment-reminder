package com.backend.features.auth.controller;

import com.backend.features.auth.dto.request.AdSystemUserUnlockRequest;
import com.backend.features.auth.dto.request.LoginRequest;
import com.backend.features.auth.dto.response.AdSystemUserUnlockResponse;
import com.backend.features.auth.dto.response.LoginResponse;
import com.backend.features.auth.service.AdAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AdAuthService adAuthService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {

        LoginResponse loginResponse = adAuthService.login(request, apiKeyHeader);

        if (loginResponse.isSuccess()) {
            return ResponseEntity.ok(loginResponse);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
        }
    }

    @PostMapping("/users/unlock")
    public ResponseEntity<AdSystemUserUnlockResponse> unlockUser(
            @Valid @RequestBody AdSystemUserUnlockRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {

        log.info("REST request to unlock AD user: {}", request.getUsername());
        AdSystemUserUnlockResponse response = adAuthService.unlockUser(request, apiKeyHeader);
        return ResponseEntity.ok(response);
    }
}
