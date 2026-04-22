package com.backend.features.auth.service;

import com.backend.enums.user.UserType;
import com.backend.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {

    private final UserRepository userRepository;

    public boolean isUsernameAvailable(String userIdentifier, UserType userType) {
        log.debug("Checking username availability: {} for type: {}", userIdentifier, userType);

        // Check global uniqueness by user type
        boolean existsByType = userRepository.existsByUserIdentifierAndUserTypeAndIsDeletedFalse(userIdentifier, userType);
        log.debug("Username {} exists for type {}: {}", userIdentifier, userType, existsByType);
        return !existsByType;
    }

    public void validateUsernameUniqueness(String userIdentifier, UserType userType) {
        if (!isUsernameAvailable(userIdentifier, userType)) {
            String context = " for " + userType.name().toLowerCase().replace("_", " ");
            throw new com.backend.exception.custom.ValidationException(
                    "Username '" + userIdentifier + "' is already taken" + context
            );
        }
    }
}
