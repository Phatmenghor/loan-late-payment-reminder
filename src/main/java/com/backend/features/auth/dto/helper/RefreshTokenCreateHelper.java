package com.backend.features.auth.dto.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Helper DTO for creating RefreshToken via MapStruct
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenCreateHelper {
    private String token;
    private UUID userId;
    private LocalDateTime expiryDate;
    private Boolean isRevoked;
    private String ipAddress;
    private String deviceInfo;
}
