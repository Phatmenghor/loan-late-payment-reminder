package com.backend.features.auth.service;

import com.backend.features.auth.models.RefreshToken;
import com.backend.features.auth.models.User;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user, String ipAddress, String deviceInfo);

    Optional<RefreshToken> verifyRefreshToken(String token);

    void revokeRefreshToken(String token, String reason);

    void revokeAllUserTokens(UUID userId, String reason);
}
