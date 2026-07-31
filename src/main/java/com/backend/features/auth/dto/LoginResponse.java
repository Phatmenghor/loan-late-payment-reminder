package com.backend.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private boolean success;
    private String message;
    private AdUserDto adUser;

    public static LoginResponse success(AdUserDto adUser) {
        return new LoginResponse(true, "Authentication successful", adUser);
    }

    public static LoginResponse success(AdUserDto adUser, String message) {
        return new LoginResponse(true, message, adUser);
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse(false, message, null);
    }
}
