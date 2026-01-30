package com.emenu.features.auth.service.impl;

import com.emenu.enums.user.AccountStatus;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.auth.dto.filter.UserFilterRequest;
import com.emenu.features.auth.dto.request.UserCreateRequest;
import com.emenu.features.auth.dto.response.UserResponse;
import com.emenu.features.auth.dto.update.UserUpdateRequest;
import com.emenu.features.auth.mapper.UserMapper;
import com.emenu.features.auth.models.Role;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.RoleRepository;
import com.emenu.features.auth.repository.UserRepository;
import com.emenu.features.auth.service.UserService;
import com.emenu.security.SecurityUtils;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import com.emenu.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.usertype.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final PaginationMapper paginationMapper;

    /**
     * Creates a new user with roles and business association
     */
    @Override
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user: {}", request.getUserIdentifier());

        if (userRepository.existsByUserIdentifierAndIsDeletedFalse(request.getUserIdentifier())) {
            throw new ValidationException("User identifier already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        List<Role> roles = roleRepository.findByNameInAndIsDeletedFalse(request.getRoles());
        if (roles.size() != request.getRoles().size()) {
            throw new ValidationException("One or more roles not found");
        }

        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("User created: {}", savedUser.getUserIdentifier());
        return userMapper.toResponse(savedUser);
    }


    /**
     * Retrieves all users with filtering and pagination support
     */
    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<UserResponse> getAllUsers(UserFilterRequest request) {

        Pageable pageable = PaginationUtils.createPageable(
                request.getPageNo(), request.getPageSize(), request.getSortBy(), request.getSortDirection()
        );

        List<AccountStatus> accountStatuses = (request.getAccountStatuses() != null && !request.getAccountStatuses().isEmpty())
                ? request.getAccountStatuses() : null;
        List<String> roles = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles() : null;

        Page<User> userPage = userRepository.searchUsers(
                accountStatuses,
                roles,
                request.getSearch(),
                pageable
        );

        return userMapper.toPaginationResponse(userPage, paginationMapper);
    }

    /**
     * Retrieves a user by ID
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toResponse(user);
    }

    /**
     * Updates an existing user
     */
    @Override
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            List<Role> roles = roleRepository.findByNameInAndIsDeletedFalse(request.getRoles());
            if (roles.size() != request.getRoles().size()) {
                throw new ValidationException("One or more roles not found");
            }

            user.getRoles().clear();
            user.getRoles().addAll(roles);
        }

        userMapper.updateEntity(request, user);
        User updatedUser = userRepository.save(user);

        log.info("User updated: {}", updatedUser.getUserIdentifier());
        return userMapper.toResponse(updatedUser);
    }

    /**
     * Soft deletes a user
     */
    @Override
    public UserResponse deleteUser(UUID userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User currentUser = securityUtils.getCurrentUser();
        if (user.getId().equals(currentUser.getId())) {
            throw new ValidationException("You cannot delete your own account");
        }

        user.softDelete();
        user = userRepository.save(user);
        log.info("User deleted: {}", user.getUserIdentifier());

        return userMapper.toResponse(user);
    }

    /**
     * Retrieves the currently authenticated user's information
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User currentUser = securityUtils.getCurrentUser();
        return userMapper.toResponse(currentUser);
    }

    /**
     * Updates the currently authenticated user's information
     */
    @Override
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        userMapper.updateEntity(request, currentUser);
        User updatedUser = userRepository.save(currentUser);
        
        log.info("Current user updated: {}", updatedUser.getUserIdentifier());
        return userMapper.toResponse(updatedUser);
    }
}