package com.backend.features.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdSystemUserUnlockResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private AdSystemUserResponse user;

    public static AdSystemUserUnlockResponse success(AdSystemUserResponse user) {
        return AdSystemUserUnlockResponse.builder()
                .success(true)
                .errorCode(null)
                .message("User account unlocked successfully to ACTIVE status")
                .user(user)
                .build();
    }

    public static AdSystemUserUnlockResponse success(AdSystemUserResponse user, String message) {
        return AdSystemUserUnlockResponse.builder()
                .success(true)
                .errorCode(null)
                .message(message)
                .user(user)
                .build();
    }

    public static AdSystemUserUnlockResponse failure(String errorCode, String message) {
        return AdSystemUserUnlockResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .user(null)
                .build();
    }
}
