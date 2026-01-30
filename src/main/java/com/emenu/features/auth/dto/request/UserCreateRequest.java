package com.emenu.features.auth.dto.request;

import com.emenu.enums.user.AccountStatus;
import com.emenu.enums.user.RoleEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "User identifier is required")
    private String userIdentifier;

    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 4, max = 100)
    private String password;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profileImageUrl;

    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @NotNull(message = "Role is required")
    private RoleEnum role;
}
