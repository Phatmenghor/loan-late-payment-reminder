package com.backend.features.auth.service.impl;

import com.backend.enums.user.UserType;
import com.backend.exception.custom.ValidationException;
import com.backend.features.auth.dto.request.AdminPasswordResetRequest;
import com.backend.features.auth.dto.request.LoginRequest;
import com.backend.features.auth.dto.request.PasswordChangeRequest;
import com.backend.features.auth.dto.request.RefreshTokenRequest;
import com.backend.features.auth.dto.request.RegisterRequest;
import com.backend.features.auth.dto.response.LoginResponse;
import com.backend.features.auth.dto.response.RefreshTokenResponse;
import com.backend.features.auth.dto.response.UserResponse;
import com.backend.features.auth.mapper.UserMapper;
import com.backend.features.auth.models.RefreshToken;
import com.backend.features.auth.models.User;
import com.backend.features.auth.repository.UserRepository;
import com.backend.features.auth.service.AuthService;
import com.backend.features.auth.service.RefreshTokenService;
import com.backend.features.auth.service.UserValidationService;
import com.backend.security.SecurityUtils;
import com.backend.security.jwt.JWTGenerator;
import com.backend.security.jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTGenerator jwtGenerator;
    private final SecurityUtils securityUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final UserValidationService userValidationService;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getUserIdentifier());

        User user = userRepository.findByUserIdentifierAndIsDeletedFalse(request.getUserIdentifier())
                .orElseThrow(() -> new ValidationException("Invalid credentials"));

        if (!request.getUserType().equals(user.getUserType())) {
            throw new ValidationException("Invalid credentials");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserIdentifier(), request.getPassword())
        );

        securityUtils.validateAccountStatus(user);

        String accessToken = jwtGenerator.generateAccessToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, null, null);

        LoginResponse response = userMapper.toLoginResponse(user, accessToken);
        response.setRefreshToken(refreshToken.getToken());

        log.info("Login successful: identifier={}, userType={}", user.getUserIdentifier(), user.getUserType());
        return response;
    }

    @Override
    public UserResponse registerCustomer(RegisterRequest request) {
        log.info("Customer registration: {}", request.getUserIdentifier());

        userValidationService.validateUsernameUniqueness(request.getUserIdentifier(), UserType.CUSTOMER);

        User user = userMapper.toEntity(request);
        user.setUserType(UserType.CUSTOMER);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        log.info("Customer registered: {}", savedUser.getUserIdentifier());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);

        if (token == null || !jwtGenerator.validateToken(token)) {
            throw new ValidationException("Invalid token");
        }

        String userIdentifier = jwtGenerator.getUsernameFromJWT(token);

        tokenBlacklistService.blacklistToken(token, userIdentifier, "LOGOUT");

        User user = userRepository.findByUserIdentifierAndIsDeletedFalse(userIdentifier)
                .orElseThrow(() -> new ValidationException("User not found"));
        refreshTokenService.revokeAllUserTokens(user.getId(), "LOGOUT");

        log.info("Logout successful: identifier={}", userIdentifier);
    }

    @Override
    public UserResponse changePassword(PasswordChangeRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Password confirmation does not match");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User savedUser = userRepository.save(currentUser);

        tokenBlacklistService.blacklistAllUserTokens(currentUser.getUserIdentifier(), "PASSWORD_CHANGE");
        refreshTokenService.revokeAllUserTokens(currentUser.getId(), "PASSWORD_CHANGE");

        log.info("Password changed: identifier={}", currentUser.getUserIdentifier());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse adminResetPassword(AdminPasswordResetRequest request) {
        log.info("Admin password reset: targetUserId={}", request.getUserId());

        User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId())
                .orElseThrow(() -> new ValidationException("User not found"));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Password confirmation does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User savedUser = userRepository.save(user);

        tokenBlacklistService.blacklistAllUserTokens(user.getUserIdentifier(), "ADMIN_PASSWORD_RESET");
        refreshTokenService.revokeAllUserTokens(user.getId(), "ADMIN_PASSWORD_RESET");

        String adminId = securityUtils.getCurrentUserOptional()
                .map(u -> u.getId().toString())
                .orElse("SYSTEM");
        log.info("Admin password reset completed: targetUserId={}, adminId={}", savedUser.getId(), adminId);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenString = request.getRefreshToken();

        if (!jwtGenerator.validateToken(refreshTokenString)) {
            throw new ValidationException("Invalid refresh token");
        }

        String userIdentifier = jwtGenerator.getUsernameFromJWT(refreshTokenString);
        String userTypeStr = jwtGenerator.getUserTypeFromJWT(refreshTokenString);
        String userRoleStr = jwtGenerator.getUserRoleFromJWT(refreshTokenString);

        log.info("Token refresh attempt: identifier={}", userIdentifier);

        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenString)
                .orElseThrow(() -> new ValidationException("Invalid or expired refresh token"));

        User user = findUserByRefreshTokenContext(userIdentifier, userTypeStr);

        if (!user.getId().equals(refreshToken.getUserId())) {
            log.error("Token refresh security violation: User ID mismatch - identifier={}", userIdentifier);
            throw new ValidationException("Invalid refresh token");
        }

        securityUtils.validateAccountStatus(user);

        // Generate new access token with role from user (not from token)
        // to ensure role is always current from database
        String newAccessToken = jwtGenerator.generateAccessTokenFromUsername(
                user.getUserIdentifier(),
                java.util.Collections.singletonList("ROLE_" + user.getUserRole().name())
        );
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, null, null);

        refreshTokenService.revokeRefreshToken(refreshTokenString, "TOKEN_REFRESH");

        log.info("Token refresh successful: identifier={}", user.getUserIdentifier());
        return new RefreshTokenResponse(newAccessToken, newRefreshToken.getToken());
    }

    private User findUserByRefreshTokenContext(String userIdentifier, String userTypeStr) {
        if (userTypeStr == null) {
            throw new ValidationException("Invalid refresh token: missing user type");
        }

        UserType userType;
        try {
            userType = UserType.valueOf(userTypeStr);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid refresh token: invalid user type");
        }

        return userRepository.findByUserIdentifierAndIsDeletedFalse(userIdentifier)
                .orElseThrow(() -> new ValidationException("User not found for refresh token context"));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        return null;
    }
}
