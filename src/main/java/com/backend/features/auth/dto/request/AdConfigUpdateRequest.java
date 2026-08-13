package com.backend.features.auth.dto.request;

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

    @NotNull(message = "adEnabled field is required")
    private Boolean adEnabled;

    private String description;
}
