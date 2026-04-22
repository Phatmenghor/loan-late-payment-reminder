package com.backend.features.auth.service.impl;

import com.backend.features.auth.dto.helper.RefreshTokenCreateHelper;
import com.backend.features.auth.mapper.RefreshTokenMapper;
import com.backend.features.auth.models.RefreshToken;
import com.backend.features.auth.models.User;
import com.backend.features.auth.repository.RefreshTokenRepository;
import com.backend.features.auth.service.RefreshTokenService;
import com.backend.security.jwt.JWTGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTGenerator jwtGenerator;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, String ipAddress, String deviceInfo) {
        log.info("Refresh token created: userId={}, identifier={}", user.getId(), user.getUserIdentifier());

        String tokenString = jwtGenerator.generateRefreshToken(
                user.getUserIdentifier(),
                user.getUserType().name(),
                user.getUserRole().name()
        );

        LocalDateTime expiryDate = LocalDateTime.ofInstant(
                jwtGenerator.getRefreshTokenExpiryDate().toInstant(),
                ZoneId.systemDefault());

        RefreshTokenCreateHelper helper = RefreshTokenCreateHelper.builder()
                .token(tokenString)
                .userId(user.getId())
                .expiryDate(expiryDate)
                .isRevoked(false)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        RefreshToken refreshToken = refreshTokenMapper.createFromHelper(helper);
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> verifyRefreshToken(String token) {
        if (!jwtGenerator.validateToken(token)) {
            log.warn("Refresh token verification failed: Invalid JWT structure");
            return Optional.empty();
        }

        if (jwtGenerator.isTokenExpired(token)) {
            log.warn("Refresh token verification failed: Token expired");
            return Optional.empty();
        }

        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenAndIsValidTrue(token);

        if (refreshTokenOpt.isEmpty()) {
            log.warn("Refresh token verification failed: Token not found or invalid");
            return Optional.empty();
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (!refreshToken.isValid()) {
            log.warn("Refresh token verification failed: Invalid state - userId={}", refreshToken.getUserId());
            return Optional.empty();
        }

        log.info("Refresh token verified: userId={}", refreshToken.getUserId());
        return Optional.of(refreshToken);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token, String reason) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            refreshToken.revoke(reason);
            refreshTokenRepository.save(refreshToken);
            log.info("Refresh token revoked: userId={}, reason={}", refreshToken.getUserId(), reason);
        } else {
            log.warn("Refresh token revocation failed: Token not found - reason={}", reason);
        }
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(UUID userId, String reason) {
        log.info("Revoking all tokens for user: userId={}, reason={}", userId, reason);

        int revokedCount = refreshTokenRepository.revokeAllByUserId(
                userId,
                LocalDateTime.now(),
                reason
        );

        log.info("Tokens revoked: userId={}, count={}, reason={}", userId, revokedCount, reason);
    }
}
