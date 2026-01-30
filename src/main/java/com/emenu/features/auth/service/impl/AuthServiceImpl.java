package com.emenu.features.auth.service.impl;

import com.emenu.enums.user.RoleEnum;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.auth.dto.request.LoginRequest;
import com.emenu.features.auth.dto.request.PasswordChangeRequest;
import com.emenu.features.auth.dto.request.RegisterRequest;
import com.emenu.features.auth.dto.response.LoginResponse;
import com.emenu.features.auth.dto.response.UserResponse;
import com.emenu.features.auth.mapper.UserMapper;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.UserRepository;
import com.emenu.features.auth.service.AuthService;
import com.emenu.security.SecurityUtils;
import com.emenu.security.jwt.JWTGenerator;
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

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getUserIdentifier());

        try {
            User user = userRepository.findByUserIdentifierAndIsDeletedFalse(request.getUserIdentifier())
                    .orElseThrow(() -> new ValidationException("Invalid credentials"));

            securityUtils.validateAccountStatus(user);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserIdentifier(), request.getPassword())
            );

            String accessToken = jwtGenerator.generateAccessToken(authentication);

            LoginResponse response = userMapper.toLoginResponse(user, accessToken);

            log.info("Login successful: {}", user.getUserIdentifier());
            return response;

        } catch (ValidationException e) {
            log.warn("Login failed: {} - Reason: {}", request.getUserIdentifier(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Login failed: {} - Error: {}", request.getUserIdentifier(), e.getMessage());
            throw new ValidationException("Invalid credentials");
        }
    }

    @Override
    public UserResponse registerCustomer(RegisterRequest request) {
        log.info("Customer registration: {}", request.getUserIdentifier());

        if (userRepository.existsByUserIdentifierAndIsDeletedFalse(request.getUserIdentifier())) {
            throw new ValidationException("User identifier already exists");
        }

        User user = userMapper.toEntity(request);
        user.setRole(RoleEnum.CUSTOMER);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        log.info("Customer registered: {}", savedUser.getUserIdentifier());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public void logout(String authorizationHeader) {
        log.info("Processing logout");
        String token = extractToken(authorizationHeader);

        if (token == null || !jwtGenerator.validateToken(token)) {
            throw new ValidationException("Invalid token");
        }

        String userIdentifier = jwtGenerator.getUsernameFromJWT(token);
        log.info("Logout successful: {}", userIdentifier);
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

        log.info("Password changed: {}", currentUser.getUserIdentifier());
        return userMapper.toResponse(savedUser);
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        return null;
    }
}
