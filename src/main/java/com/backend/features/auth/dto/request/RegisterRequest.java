package com.backend.features.auth.dto.request;

import com.backend.enums.user.AccountStatus;
import com.backend.enums.user.UserRole;
import com.backend.enums.user.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "User identifier is required")
    private String userIdentifier;
    
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8)
    private String password;

    @NotNull(message = "User type is required")
    private UserType userType = UserType.CUSTOMER;

    @NotNull(message = "User role is required")
    private UserRole userRole = UserRole.CUSTOMER;

    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String phoneNumber;
    private String address;
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
}