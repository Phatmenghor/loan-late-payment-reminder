package com.backend.features.auth.dto.response;

import com.backend.enums.common.AdUserStatus;
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
public class AdSystemUserResponse {

    private UUID id;
    private String apiKey;
    private String username;
    private AdUserStatus status;
    private LocalDateTime lastLoginAt;
    private String lockReason;
    private LocalDateTime lockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
