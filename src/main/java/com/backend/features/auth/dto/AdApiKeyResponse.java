package com.backend.features.auth.dto;

import com.backend.enums.common.Status;
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
public class AdApiKeyResponse {

    private UUID id;
    private String apiKey;
    private String label;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
