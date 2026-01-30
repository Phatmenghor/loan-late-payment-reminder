package com.emenu.features.auth.dto.request;

import com.emenu.enums.user.AccountStatus;
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

    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String phoneNumber;
    private String address;
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
}