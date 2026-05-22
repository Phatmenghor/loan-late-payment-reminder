package com.backend.features.auth.controller;

import com.backend.features.auth.dto.LoginRequest;
import com.backend.features.auth.dto.LoginResponse;
import com.backend.features.auth.service.AdAuthService;
import com.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AdAuthService adAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        LoginResponse loginResponse = adAuthService.login(request);

        if (loginResponse.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
        } else {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication failed", loginResponse));
        }
    }
}
