package com.backend.features.auth.controller;

import com.backend.features.auth.dto.LoginRequest;
import com.backend.features.auth.dto.LoginResponse;
import com.backend.features.auth.service.AdAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Active Directory Authentication API — Public Endpoint requiring System X-API-Key Header")
public class AuthController {

    private final AdAuthService adAuthService;

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate Active Directory User",
        description = "Validates caller application identity via required HTTP Header X-API-Key and performs Active Directory authentication"
    )
    @SecurityRequirement(name = "apiKey")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(description = "AD System API Key header (X-API-Key) for application identification", required = true)
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        log.info("AD Login attempt for user: {} [X-API-Key header present: {}]", request.getUsername(), apiKeyHeader != null);

        LoginResponse loginResponse = adAuthService.login(request, apiKeyHeader);

        if (loginResponse.isSuccess()) {
            return ResponseEntity.ok(loginResponse);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
        }
    }
}
