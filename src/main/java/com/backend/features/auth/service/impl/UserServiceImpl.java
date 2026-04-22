package com.backend.features.auth.service.impl;

import com.backend.enums.user.AccountStatus;
import com.backend.exception.custom.ValidationException;
import com.backend.features.auth.dto.filter.UserFilterRequest;
import com.backend.features.auth.dto.request.*;
import com.backend.features.auth.dto.response.UserDetailResponse;
import com.backend.features.auth.dto.response.UserResponse;
import com.backend.features.auth.dto.update.UserUpdateRequest;
import com.backend.features.auth.mapper.UserMapper;
import com.backend.features.auth.models.*;
import com.backend.features.auth.repository.UserRepository;
import com.backend.features.auth.service.UserService;
import com.backend.security.SecurityUtils;
import com.backend.shared.dto.PaginationResponse;
import com.backend.shared.mapper.PaginationMapper;
import com.backend.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final PaginationMapper paginationMapper;

    @Override
    public UserResponse createUser(UserCreateRequest req) {
        log.info("Creating user: identifier={}, userType={}", req.getUserIdentifier(), req.getUserType());

        if (userRepository.existsByUserIdentifierAndIsDeletedFalse(req.getUserIdentifier())) {
            throw new ValidationException("User identifier already exists");
        }

        User user = userMapper.toEntity(req);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        User saved = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(saved);
        profile.setEmail(req.getEmail());
        profile.setFirstName(req.getFirstName());
        profile.setLastName(req.getLastName());
        profile.setNickname(req.getNickname());
        profile.setGender(req.getGender());
        profile.setDateOfBirth(req.getDateOfBirth());
        profile.setPhoneNumber(req.getPhoneNumber());
        profile.setProfileImageUrl(req.getProfileImageUrl());
        saved.setProfile(profile);

        saved = userRepository.save(saved);

        log.info("User created: userId={}, identifier={}, userType={}", saved.getId(), saved.getUserIdentifier(), saved.getUserType());

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<UserResponse> getAllUsers(UserFilterRequest request) {
        Pageable pageable = PaginationUtils.createPageable(
                request.getPageNo(), request.getPageSize(), request.getSortBy(), request.getSortDirection());

        Page<User> page = userRepository.searchUsers(
                (request.getUserTypes() != null && !request.getUserTypes().isEmpty()) ? request.getUserTypes() : null,
                (request.getAccountStatuses() != null && !request.getAccountStatuses().isEmpty()) ? request.getAccountStatuses() : null,
                null,
                request.getSearch(),
                pageable);

        log.info("Users retrieved: total={}, pages={}, current={}", page.getTotalElements(), page.getTotalPages(), page.getNumber());
        return userMapper.toPaginationResponse(page, paginationMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(UUID userId) {
        return userMapper.toDetailResponse(userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
    }

    @Override
    public UserResponse updateUser(UUID userId, UserUpdateRequest req) {
        log.info("Updating user: userId={}", userId);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AccountStatus previousStatus = user.getAccountStatus();

        userMapper.updateEntity(req, user);

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        if (req.getEmail() != null) profile.setEmail(req.getEmail());
        if (req.getFirstName() != null) profile.setFirstName(req.getFirstName());
        if (req.getLastName() != null) profile.setLastName(req.getLastName());
        if (req.getNickname() != null) profile.setNickname(req.getNickname());
        if (req.getGender() != null) profile.setGender(req.getGender());
        if (req.getDateOfBirth() != null) profile.setDateOfBirth(req.getDateOfBirth());
        if (req.getPhoneNumber() != null) profile.setPhoneNumber(req.getPhoneNumber());
        if (req.getProfileImageUrl() != null) profile.setProfileImageUrl(req.getProfileImageUrl());

        User updated = userRepository.save(user);

        if (!previousStatus.equals(updated.getAccountStatus())) {
            log.info("User status changed: userId={}, from={}, to={}", userId, previousStatus, updated.getAccountStatus());
        }

        log.info("User updated: userId={}, identifier={}", updated.getId(), updated.getUserIdentifier());
        return userMapper.toResponse(updated);
    }

    @Override
    public UserResponse deleteUser(UUID userId) {
        log.info("Deleting user: userId={}", userId);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User currentUser = securityUtils.getCurrentUser();
        if (user.getId().equals(currentUser.getId())) {
            throw new ValidationException("You cannot delete your own account");
        }

        user.softDelete();
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User currentUser = securityUtils.getCurrentUser();
        log.info("Current user accessed: userId={}, identifier={}", currentUser.getId(), currentUser.getUserIdentifier());
        return userMapper.toResponse(currentUser);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        return updateUser(currentUser.getId(), request);
    }
}
