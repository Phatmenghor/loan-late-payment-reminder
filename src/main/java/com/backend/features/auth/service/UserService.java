package com.backend.features.auth.service;

import com.backend.features.auth.dto.filter.UserFilterRequest;
import com.backend.features.auth.dto.request.UserCreateRequest;
import com.backend.features.auth.dto.response.UserDetailResponse;
import com.backend.features.auth.dto.response.UserResponse;
import com.backend.features.auth.dto.update.UserUpdateRequest;
import com.backend.shared.dto.PaginationResponse;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    PaginationResponse<UserResponse> getAllUsers(UserFilterRequest request);

    UserDetailResponse getUserById(UUID userId);
    
    UserResponse updateUser(UUID userId, UserUpdateRequest request);
    
    UserResponse deleteUser(UUID userId);
    
    UserResponse getCurrentUser();

    /**
     * Updates the current authenticated user's profile.
     * Convenience method that extracts the current user ID internally.
     */
    UserResponse updateCurrentUser(UserUpdateRequest request);
}