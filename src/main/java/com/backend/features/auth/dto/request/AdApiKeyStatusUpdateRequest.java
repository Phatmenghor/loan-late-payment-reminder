package com.backend.features.auth.dto.request;

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

    @NotNull(message = "Status is required")
    private Status status;
}
