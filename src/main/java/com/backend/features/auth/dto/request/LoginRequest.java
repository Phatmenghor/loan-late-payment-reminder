package com.backend.features.auth.dto.request;

import com.backend.enums.user.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Login request with user type for disambiguation.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "User identifier is required")
    private String userIdentifier;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "User type is required")
    private UserType userType;
}