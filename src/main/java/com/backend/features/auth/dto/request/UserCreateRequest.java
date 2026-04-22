package com.backend.features.auth.dto.request;

import com.backend.enums.user.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserCreateRequest {

    // Account
    @NotBlank(message = "User identifier is required")
    private String userIdentifier;

    @NotBlank(message = "Password is required")
    @Size(min = 4, max = 100)
    private String password;

    @NotNull(message = "User type is required")
    private UserType userType;

    @NotNull(message = "User role is required")
    private UserRole userRole;

    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    private String remark;

    // Personal
    private String email;
    private String firstName;
    private String lastName;
    private String nickname;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String profileImageUrl;
}
