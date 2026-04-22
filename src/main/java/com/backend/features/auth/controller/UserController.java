package com.backend.features.auth.controller;

import com.backend.features.auth.dto.filter.UserFilterRequest;
import com.backend.features.auth.dto.request.AdminPasswordResetRequest;
import com.backend.features.auth.dto.request.PasswordChangeRequest;
import com.backend.features.auth.dto.request.UserCreateRequest;
import com.backend.features.auth.dto.response.UserDetailResponse;
import com.backend.features.auth.dto.response.UserResponse;
import com.backend.features.auth.dto.update.UserUpdateRequest;
import com.backend.features.auth.service.AuthService;
import com.backend.features.auth.service.UserService;
import com.backend.shared.dto.ApiResponse;
import com.backend.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Retrieves a test business owner token for development purposes
     */
    @PostMapping("owner-token")
    public ResponseEntity<String> getMyBusinessToken() {
        log.info("Get my business token");
        return ResponseEntity.ok("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwaGF0bWVuZ2hvcjE5QGdtYWlsLmNvbSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3NzU2NTA3ODIsImV4cCI6MTc4NDI5MDc4Mn0.LJpacRTlp5bdcqw1PXbROfW1Id3v488GWcI9bOK5b0eahZWUWXq19_Ya4wPGZHc7VCp6vVO0tQmVYAkstCZOuA");
    }

    /**
     * Retrieves a test business owner token for development purpo
     */
    @PostMapping("customer-token")
    public ResponseEntity<String> getMyCustomerToken() {
        log.info("Get my customer token");
        return ResponseEntity.ok("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwaGF0bWVuZ2hvcjIxQGdtYWlsLmNvbSIsInJvbGVzIjoiUk9MRV9DVVNUT01FUiIsInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3NzU3Mzc0NzYsImV4cCI6MTc4NDM3NzQ3Nn0.aW1-49PCFKRxHTZOrfnbsSA-k8EuEck_X0738zuNTaaayNWnCchE-rn8QwHeVnBuCWyRe7PtMLganhIjVJ6Akg");
    }

    /**
     * Retrieves the current authenticated user's profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        log.info("Get current user profile");
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", response));
    }

    /**
     * Updates the current authenticated user's profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Update current user profile");
        UserResponse response = userService.updateCurrentUser(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    /**
     * Retrieves all users with pagination and filtering
     */
    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getAllUsers(
            @Valid @RequestBody UserFilterRequest request) {
        log.info("Get all users");
        PaginationResponse<UserResponse> response = userService.getAllUsers(request);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", response));
    }

    /**
     * Retrieves a user by their ID with full details
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable UUID userId) {
        log.info("Get user: {}", userId);
        UserDetailResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", response));
    }

    /**
     * Creates a new user
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        log.info("Create user: {}", request.getUserIdentifier());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", response));
    }

    /**
     * Updates an existing user
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Update user: {}", userId);
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated", response));
    }

    /**
     * Deletes a user by their ID
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> deleteUser(@PathVariable UUID userId) {
        log.info("Delete user: {}", userId);
        UserResponse response = userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted", response));
    }

    /**
     * Allows an admin to reset a user's password
     */
    @PostMapping("/admin/reset-password")
    public ResponseEntity<ApiResponse<UserResponse>> adminResetPassword(
            @Valid @RequestBody AdminPasswordResetRequest request) {
        log.info("Admin password reset: {}", request.getUserId());
        UserResponse response = authService.adminResetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", response));
    }

    /**
     * Allows a user to change their own password
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request) {
        log.info("Password change request");
        UserResponse response = authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", response));
    }

    /**
     * Logs out the current user by invalidating their token
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

}
