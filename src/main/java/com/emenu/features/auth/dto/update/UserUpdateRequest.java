package com.emenu.features.auth.dto.update;

import com.emenu.enums.user.AccountStatus;
import com.emenu.enums.user.RoleEnum;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profileImageUrl;
    private AccountStatus accountStatus;
    private RoleEnum role;
}
