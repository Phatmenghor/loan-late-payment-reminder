package com.emenu.features.auth.controller;

import com.emenu.features.auth.dto.filter.UserFilterRequest;
import com.emenu.features.auth.dto.request.AdminPasswordResetRequest;
import com.emenu.features.auth.dto.request.PasswordChangeRequest;
import com.emenu.features.auth.dto.request.UserCreateRequest;
import com.emenu.features.auth.dto.response.UserResponse;
import com.emenu.features.auth.dto.update.UserUpdateRequest;
import com.emenu.features.auth.service.AuthService;
import com.emenu.features.auth.service.UserService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
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
     * Retrieves a test admin token for development purposes
     */
    @PostMapping("admin-token")
    public ResponseEntity<String> getMyAdminToken() {
        log.info("Get my admin token");
        return ResponseEntity.ok("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwaGF0bWVuZ2hvcjE5QGdtYWlsLmNvbSIsInJvbGVzIjoiUk9MRV9QTEFURk9STV9PV05FUiIsInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3NjkxNDIzNzksImV4cCI6MTc3MDI1MzQ5MH0.DDIFoUmOSCsiymW-565oReXGU5_VIRL6Soiige94oL39PKLb3ZhdDdxyoxAtwinq2qHAo_LzLzEyenJqjz21qA");
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
     * Retrieves a user by their ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        log.info("Get user: {}", userId);
        UserResponse response = userService.getUserById(userId);
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
