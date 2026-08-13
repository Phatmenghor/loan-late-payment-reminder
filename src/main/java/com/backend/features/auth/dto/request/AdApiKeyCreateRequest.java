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
public class AdApiKeyCreateRequest {

    @NotBlank(message = "Label is required")
    private String label;

    private String description;
}
