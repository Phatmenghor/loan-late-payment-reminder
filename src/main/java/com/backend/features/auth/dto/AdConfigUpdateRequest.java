package com.backend.features.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdConfigUpdateRequest {

    @NotNull(message = "adEnabled parameter is required (true or false)")
    private Boolean adEnabled;

    private String description;
}
