package com.backend.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdSystemUserUnlockRequest {

    @NotBlank(message = "Username is required")
    private String username;

    private String reason;
}
