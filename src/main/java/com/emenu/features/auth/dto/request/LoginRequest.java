package com.emenu.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Login request with context for dynamic username uniqueness.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "User identifier is required")
    private String userIdentifier;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "User type is required")
    private UserType userType;

    private UUID businessId;
}