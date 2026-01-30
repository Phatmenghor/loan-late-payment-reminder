package com.emenu.security;

import com.emenu.enums.user.AccountStatus;
import com.emenu.enums.user.RoleEnum;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("User not authenticated");
        }

        String userIdentifier = authentication.getName();
        return userRepository.findByUserIdentifierAndIsDeletedFalse(userIdentifier)
                .orElseThrow(() -> new ValidationException("User not found"));
    }

    public String getCurrentUserIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }

    public void validateAccountStatus(User user) {

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new ValidationException("Account is inactive");
        }
    }

    public boolean isCurrentUser(String userIdentifier) {
        String currentUserIdentifier = getCurrentUserIdentifier();
        return currentUserIdentifier != null && currentUserIdentifier.equals(userIdentifier);
    }

    public Optional<User> getCurrentUserOptional() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null ||
                    !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                log.debug("No authenticated user - public access");
                return Optional.empty();
            }

            String userIdentifier = authentication.getName();
            Optional<User> userOpt = userRepository.findByUserIdentifierAndIsDeletedFalse(userIdentifier);

            if (userOpt.isEmpty()) {
                log.warn("Authenticated user not found in database: {}", userIdentifier);
                return Optional.empty();
            }

            User user = userOpt.get();

            try {
                validateAccountStatus(user);
            } catch (Exception e) {
                log.warn("User account validation failed: {} - {}", userIdentifier, e.getMessage());
                return Optional.empty();
            }

            return Optional.of(user);

        } catch (Exception e) {
            log.debug("Error getting current user (public access mode): {}", e.getMessage());
            return Optional.empty();
        }
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public RoleEnum getCurrentUserRole() {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getRole();
        } catch (Exception e) {
            log.debug("Error getting user role: {}", e.getMessage());
            return null;
        }
    }
}
