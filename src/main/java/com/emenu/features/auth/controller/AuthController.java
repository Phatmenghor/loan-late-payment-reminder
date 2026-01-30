package com.emenu.features.auth.controller;

import com.emenu.features.auth.dto.request.LoginRequest;
import com.emenu.features.auth.dto.request.PasswordChangeRequest;
import com.emenu.features.auth.dto.request.RegisterRequest;
import com.emenu.features.auth.dto.response.LoginResponse;
import com.emenu.features.auth.dto.response.UserResponse;
import com.emenu.features.auth.service.AuthService;
import com.emenu.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request: {}", request.getUserIdentifier());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Customer registration: {}", request.getUserIdentifier());
        UserResponse response = authService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        log.info("Logout request");
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        log.info("Password change request");
        UserResponse response = authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", response));
    }
}
