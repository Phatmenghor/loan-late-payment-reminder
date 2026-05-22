package com.backend.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private boolean success;
    private String message;
    private Map<String, Object> adAttributes;

    public static LoginResponse success(Map<String, Object> adAttributes) {
        return new LoginResponse(true, "Authentication successful", adAttributes);
    }

    public static LoginResponse success(Map<String, Object> adAttributes, String message) {
        return new LoginResponse(true, message, adAttributes);
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse(false, message, null);
    }
}
