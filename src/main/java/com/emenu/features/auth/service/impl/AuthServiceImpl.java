package com.emenu.features.auth.service.impl;

import com.emenu.exception.custom.ValidationException;
import com.emenu.features.auth.dto.request.AdminPasswordResetRequest;
import com.emenu.features.auth.dto.request.LoginRequest;
import com.emenu.features.auth.dto.request.PasswordChangeRequest;
import com.emenu.features.auth.dto.request.RefreshTokenRequest;
import com.emenu.features.auth.dto.request.RegisterRequest;
import com.emenu.features.auth.dto.response.LoginResponse;
import com.emenu.features.auth.dto.response.RefreshTokenResponse;
import com.emenu.features.auth.dto.response.UserResponse;
import com.emenu.features.auth.mapper.UserMapper;
import com.emenu.features.auth.models.RefreshToken;
import com.emenu.features.auth.models.Role;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.RoleRepository;
import com.emenu.features.auth.repository.UserRepository;
import com.emenu.features.auth.service.AuthService;
import com.emenu.features.auth.service.RefreshTokenService;
import com.emenu.security.SecurityUtils;
import com.emenu.security.jwt.JWTGenerator;
import com.emenu.security.jwt.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.usertype.UserType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTGenerator jwtGenerator;
    private final SecurityUtils securityUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Authenticates a user and generates a JWT token with context-aware user lookup
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: {} (userType: {}, businessId: {})",
                request.getUserIdentifier(), request.getUserType(), request.getBusinessId());

        try {
            // Find user with context-aware lookup
            User user = findUserWithContext(request);

            // Validate context matches (if provided)
            validateLoginContext(request, user);

            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserIdentifier(), request.getPassword())
            );

            // Validate account status
            securityUtils.validateAccountStatus(user);

            // Generate access token
            String accessToken = jwtGenerator.generateAccessToken(authentication);

            // Generate refresh token
            String ipAddress = getClientIpAddress();
            String deviceInfo = getDeviceInfo();
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, deviceInfo);

            // Create user session for device tracking
            HttpServletRequest httpRequest = getHttpServletRequest();
            if (httpRequest != null) {
                userSessionService.createSession(user, refreshToken, httpRequest);
            }

            // Build login response
            LoginResponse response = userMapper.toLoginResponse(user, accessToken);
            response.setRefreshToken(refreshToken.getToken());

            // Add business subscription info for business users
            if (user.isBusinessUser() && user.getBusinessId() != null) {
                Business business = businessRepository.findById(user.getBusinessId()).orElse(null);
                if (business != null) {
                    response.setBusinessStatus(business.getStatus().toString());
                    response.setIsSubscriptionActive(business.hasActiveSubscription());
                }
            }

            log.info("Login successful: {} (type: {}, businessId: {})",
                    user.getUserIdentifier(), user.getUserType(), user.getBusinessId());
            return response;

        } catch (ValidationException e) {
            log.warn("Login failed: {} - Reason: {}", request.getUserIdentifier(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Login failed: {} - Error: {}", request.getUserIdentifier(), e.getMessage());
            throw new ValidationException("Invalid credentials");
        }
    }

    /**
     * Find user with context-aware lookup based on userType and businessId.
     * UserType is REQUIRED to disambiguate which user account to authenticate.
     */
    private User findUserWithContext(LoginRequest request) {
        String userIdentifier = request.getUserIdentifier();
        UserType userType = request.getUserType();
        UUID businessId = request.getBusinessId();

        // Validate userType is provided (should be caught by @NotNull, but double-check)
        if (userType == null) {
            throw new ValidationException(
                    "User type is required. Please specify whether you are logging in as CUSTOMER, PLATFORM_USER, or BUSINESS_USER."
            );
        }

        // Case 1: BUSINESS_USER - requires businessId
        if (userType == UserType.BUSINESS_USER) {
            if (businessId == null) {
                throw new ValidationException(
                        "Business ID is required for business user login. Please provide businessId in your login request."
                );
            }

            log.debug("Looking up business user: {} in business: {}", userIdentifier, businessId);
            return userRepository.findByUserIdentifierAndBusinessIdAndIsDeletedFalse(userIdentifier, businessId)
                    .orElseThrow(() -> new ValidationException(
                            "User '" + userIdentifier + "' not found in the specified business"
                    ));
        }

        // Case 2: CUSTOMER or PLATFORM_USER - global uniqueness per type
        log.debug("Looking up {} user: {}", userType, userIdentifier);
        return userRepository.findByUserIdentifierAndUserTypeAndIsDeletedFalse(userIdentifier, userType)
                .orElseThrow(() -> new ValidationException(
                        "User '" + userIdentifier + "' not found as " + userType.name().toLowerCase().replace("_", " ")
                ));
    }

    /**
     * Validate that the found user matches the requested context.
     * This is a safety check - the findUserWithContext method should already ensure correct user.
     */
    private void validateLoginContext(LoginRequest request, User user) {
        // Validate userType matches
        if (!request.getUserType().equals(user.getUserType())) {
            throw new ValidationException(
                    "User type mismatch. Expected: " + request.getUserType() +
                            ", Found: " + user.getUserType()
            );
        }

        // Validate businessId for business users
        if (request.getUserType() == UserType.BUSINESS_USER) {
            if (user.getBusinessId() == null) {
                throw new ValidationException("User is not associated with any business");
            }
            if (!request.getBusinessId().equals(user.getBusinessId())) {
                throw new ValidationException("User does not belong to the specified business");
            }
        }
    }

    /**
     * Registers a new customer user
     */
    @Override
    public UserResponse registerCustomer(RegisterRequest request) {
        log.info("Customer registration: {}", request.getUserIdentifier());

        // Validate username uniqueness for CUSTOMER type (global uniqueness among customers)
        userValidationService.validateUsernameUniqueness(
                request.getUserIdentifier(),
                UserType.CUSTOMER,
                null
        );

        User user = userMapper.toEntity(request);
        user.setUserType(UserType.CUSTOMER);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role customerRole = roleRepository.findByNameAndIsDeletedFalse("CUSTOMER")
                .orElseThrow(() -> new ValidationException("Customer role not found"));

        // Validate role is compatible with CUSTOMER user type
        if (!customerRole.isCompatibleWithUserType(UserType.CUSTOMER)) {
            throw new ValidationException("CUSTOMER role is not properly configured for CUSTOMER user type");
        }

        user.setRoles(List.of(customerRole));

        User savedUser = userRepository.save(user);

        log.info("Customer registered: {}", savedUser.getUserIdentifier());
        return userMapper.toResponse(savedUser);
    }

    /**
     * Logs out a user by blacklisting their JWT token and revoking refresh token
     */
    @Override
    public void logout(String authorizationHeader) {
        log.info("Processing logout");
        String token = extractToken(authorizationHeader);

        if (token == null || !jwtGenerator.validateToken(token)) {
            throw new ValidationException("Invalid token");
        }

        String userIdentifier = jwtGenerator.getUsernameFromJWT(token);

        // Blacklist access token
        tokenBlacklistService.blacklistToken(token, userIdentifier, "LOGOUT");

        // Revoke all refresh tokens for the user
        User user = userRepository.findByUserIdentifierAndIsDeletedFalse(userIdentifier)
                .orElseThrow(() -> new ValidationException("User not found"));
        refreshTokenService.revokeAllUserTokens(user.getId(), "LOGOUT");

        log.info("Logout successful: {}", userIdentifier);
    }

    /**
     * Changes the password for the currently authenticated user
     */
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

        // Blacklist all access tokens
        tokenBlacklistService.blacklistAllUserTokens(currentUser.getUserIdentifier(), "PASSWORD_CHANGE");

        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(currentUser.getId(), "PASSWORD_CHANGE");

        log.info("Password changed: {}", currentUser.getUserIdentifier());

        return userMapper.toResponse(savedUser);
    }

    /**
     * Resets a user's password (admin function)
     */
    @Override
    public UserResponse adminResetPassword(AdminPasswordResetRequest request) {
        log.info("Admin password reset: {}", request.getUserId());

        User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId())
                .orElseThrow(() -> new ValidationException("User not found"));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Password confirmation does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User savedUser = userRepository.save(user);

        // Blacklist all access tokens
        tokenBlacklistService.blacklistAllUserTokens(user.getUserIdentifier(), "ADMIN_PASSWORD_RESET");

        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(user.getId(), "ADMIN_PASSWORD_RESET");

        log.info("Admin password reset: {}", user.getUserIdentifier());

        return userMapper.toResponse(savedUser);
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        return null;
    }

    /**
     * Get HttpServletRequest from RequestContextHolder
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.warn("Failed to get HttpServletRequest", e);
        }
        return null;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress() {
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "Unknown";
    }

    /**
     * Get device info from request
     */
    private String getDeviceInfo() {
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            return request.getHeader("User-Agent");
        }
        return "Unknown";
    }

    /**
     * Refresh access token using refresh token
     */
    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Processing refresh token request");

        String refreshTokenString = request.getRefreshToken();

        // Verify refresh token JWT structure first
        if (!jwtGenerator.validateToken(refreshTokenString)) {
            throw new ValidationException("Invalid refresh token");
        }

        // Extract user context from refresh token JWT
        String userIdentifier = jwtGenerator.getUsernameFromJWT(refreshTokenString);
        String userTypeStr = jwtGenerator.getUserTypeFromJWT(refreshTokenString);
        String businessIdStr = jwtGenerator.getBusinessIdFromJWT(refreshTokenString);

        log.info("Refresh token context: userIdentifier={}, userType={}, businessId={}",
                userIdentifier, userTypeStr, businessIdStr);

        // Verify refresh token exists in database and is valid
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenString)
                .orElseThrow(() -> new ValidationException("Invalid or expired refresh token"));

        // Find user using context from refresh token JWT
        User user = findUserByRefreshTokenContext(userIdentifier, userTypeStr, businessIdStr);

        // Validate that the found user matches the refresh token's userId
        if (!user.getId().equals(refreshToken.getUserId())) {
            log.error("Security: User ID mismatch! Token userId={}, Found userId={}",
                    refreshToken.getUserId(), user.getId());
            throw new ValidationException("Invalid refresh token");
        }

        // Validate account status
        securityUtils.validateAccountStatus(user);

        // Validate business subscription and status for business users
        if (user.isBusinessUser() && user.getBusinessId() != null) {
            Business business = businessRepository.findById(user.getBusinessId())
                    .orElseThrow(() -> new ValidationException("Business not found"));

            if (!business.isActive()) {
                log.warn("Refresh denied: Business is not active - {}", business.getStatus());
                throw new ValidationException("Your business account is currently " + business.getStatus() + ". Please contact support.");
            }

            if (!business.hasActiveSubscription()) {
                log.warn("Refresh denied: Business subscription is not active");
                throw new ValidationException("Your business subscription has expired. Please renew your subscription to continue.");
            }
        }

        // Get user roles
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        // Generate new access token
        String newAccessToken = jwtGenerator.generateAccessTokenFromUsername(user.getUserIdentifier(), roles);

        // Generate a new refresh token (rotate refresh tokens for better security)
        String ipAddress = getClientIpAddress();
        String deviceInfo = getDeviceInfo();
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, ipAddress, deviceInfo);

        // Create/update user session for device tracking
        HttpServletRequest httpRequest = getHttpServletRequest();
        if (httpRequest != null) {
            userSessionService.createSession(user, newRefreshToken, httpRequest);
        }

        // Revoke old refresh token
        refreshTokenService.revokeRefreshToken(refreshTokenString, "TOKEN_REFRESH");

        log.info("Token refresh successful: {} (type: {}, businessId: {})",
                user.getUserIdentifier(), user.getUserType(), user.getBusinessId());

        return new RefreshTokenResponse(newAccessToken, newRefreshToken.getToken());
    }

    /**
     * Find user by refresh token context (userIdentifier, userType, businessId)
     */
    private User findUserByRefreshTokenContext(String userIdentifier, String userTypeStr, String businessIdStr) {
        if (userTypeStr == null) {
            throw new ValidationException("Invalid refresh token: missing user type");
        }

        UserType userType;
        try {
            userType = UserType.valueOf(userTypeStr);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid refresh token: invalid user type");
        }

        // For BUSINESS_USER, businessId is required
        if (userType == UserType.BUSINESS_USER) {
            if (businessIdStr == null) {
                throw new ValidationException("Invalid refresh token: missing business ID for business user");
            }

            UUID businessId;
            try {
                businessId = UUID.fromString(businessIdStr);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid refresh token: invalid business ID format");
            }

            return userRepository.findByUserIdentifierAndBusinessIdAndIsDeletedFalse(userIdentifier, businessId)
                    .orElseThrow(() -> new ValidationException("User not found for refresh token context"));
        }

        // For CUSTOMER and PLATFORM_USER
        return userRepository.findByUserIdentifierAndUserTypeAndIsDeletedFalse(userIdentifier, userType)
                .orElseThrow(() -> new ValidationException("User not found for refresh token context"));
    }
}