package com.backend.features.auth.dto.response;

import com.backend.enums.user.AccountStatus;
import com.backend.enums.user.Gender;
import com.backend.enums.user.UserRole;
import com.backend.enums.user.UserType;
import com.backend.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserResponse extends BaseAuditResponse {

    // ── Account ────────────────────────────────────────────────────────────
    private String userIdentifier;
    private UserType userType;
    private UserRole userRole;
    private AccountStatus accountStatus;
    private String remark;

    // ── Personal (from user_profiles) ─────────────────────────────────────
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String nickname;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String profileImageUrl;
}
