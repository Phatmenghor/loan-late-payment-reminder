package com.backend.features.auth.dto;

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
public class AdLogResponse {
    private UUID id;
    private String username;
    private Boolean success;
    private String failureReason;
    private LocalDateTime calledAt;
}
