package com.backend.features.auth.service;

import com.backend.features.auth.dto.request.AdminPasswordResetRequest;
import com.backend.features.auth.dto.request.LoginRequest;
import com.backend.features.auth.dto.request.PasswordChangeRequest;
import com.backend.features.auth.dto.request.RefreshTokenRequest;
import com.backend.features.auth.dto.request.RegisterRequest;
import com.backend.features.auth.dto.response.LoginResponse;
import com.backend.features.auth.dto.response.RefreshTokenResponse;
import com.backend.features.auth.dto.response.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String token);

    UserResponse registerCustomer(RegisterRequest request);

    UserResponse changePassword(PasswordChangeRequest request);

    UserResponse adminResetPassword(AdminPasswordResetRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);
}