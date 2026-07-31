package com.backend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private Boolean success;
    private String status;
    private String message;
    private T data;

    public ApiResponse(String status, String message, T data) {
        this.success = "success".equalsIgnoreCase(status);
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "success", message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, "error", message, null);
    }

    public static <T> ApiResponse<T> error(String message, T errorData) {
        return new ApiResponse<>(false, "error", message, errorData);
    }
}