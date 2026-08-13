package com.backend.features.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private AdUserDto adUser;

    public static LoginResponse success(AdUserDto adUser) {
        return LoginResponse.builder()
                .success(true)
                .errorCode(null)
                .message("Authentication successful")
                .adUser(adUser)
                .build();
    }

    public static LoginResponse success(AdUserDto adUser, String customMessage) {
        return LoginResponse.builder()
                .success(true)
                .errorCode(null)
                .message(customMessage)
                .adUser(adUser)
                .build();
    }

    public static LoginResponse failure(String errorCode, String message) {
        return LoginResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .adUser(null)
                .build();
    }

    public static LoginResponse failure(String message) {
        return LoginResponse.builder()
                .success(false)
                .errorCode("AUTH_FAILED")
                .message(message)
                .adUser(null)
                .build();
    }
}
