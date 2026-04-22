package com.backend.features.auth.dto.response;

import com.backend.enums.user.UserRole;
import com.backend.enums.user.UserType;
import lombok.Data;

import java.util.UUID;

@Data
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private UUID userId;
    private String userIdentifier;
    private String email;
    private String fullName;
    private String profileImageUrl;
    private UserType userType;
    private UserRole userRole;
}