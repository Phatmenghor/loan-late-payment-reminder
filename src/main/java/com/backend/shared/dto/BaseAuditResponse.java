package com.backend.shared.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public abstract class BaseAuditResponse {
    private UUID id;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
