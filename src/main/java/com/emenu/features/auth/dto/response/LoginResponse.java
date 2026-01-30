package com.emenu.features.auth.dto.response;

import com.emenu.enums.user.RoleEnum;
import lombok.Data;

import java.util.UUID;

@Data
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private UUID userId;
    private String userIdentifier;
    private String email;
    private String fullName;
    private String profileImageUrl;
    private RoleEnum role;
}
