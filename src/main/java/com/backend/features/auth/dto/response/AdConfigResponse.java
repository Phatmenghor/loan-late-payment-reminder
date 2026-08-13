package com.backend.features.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdConfigResponse {

    private UUID id;
    private Boolean adEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
