package com.emenu.features.auth.controller;

import com.emenu.features.auth.dto.filter.UserFilterRequest;
import com.emenu.features.auth.dto.request.UserCreateRequest;
import com.emenu.features.auth.dto.response.UserResponse;
import com.emenu.features.auth.dto.update.UserUpdateRequest;
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

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        log.info("Get current user profile");
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Update current user profile");
        UserResponse response = userService.updateCurrentUser(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getAllUsers(
            @Valid @RequestBody UserFilterRequest request) {
        log.info("Get all users");
        PaginationResponse<UserResponse> response = userService.getAllUsers(request);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", response));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        log.info("Get user: {}", userId);
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        log.info("Create user: {}", request.getUserIdentifier());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", response));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Update user: {}", userId);
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated", response));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> deleteUser(@PathVariable UUID userId) {
        log.info("Delete user: {}", userId);
        UserResponse response = userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted", response));
    }
}
