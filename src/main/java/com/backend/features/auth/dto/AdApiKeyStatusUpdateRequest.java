package com.backend.features.auth.dto;

import com.backend.enums.common.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdApiKeyStatusUpdateRequest {

    @NotNull(message = "Status is required (ACTIVE or INACTIVE)")
    private Status status;
}
