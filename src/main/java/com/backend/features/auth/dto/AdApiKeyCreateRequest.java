package com.backend.features.auth.dto;

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

    @NotBlank(message = "Label is required (e.g. Core Banking System, E-Commerce App)")
    private String label;
}
