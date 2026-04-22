package com.backend.features.auth.dto.update;

import com.backend.enums.user.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {

    // Account
    private AccountStatus accountStatus;
    private UserRole userRole;
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
