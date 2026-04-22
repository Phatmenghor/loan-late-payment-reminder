package com.backend.security.jwt.impl;

import com.backend.features.auth.models.BlacklistedToken;
import com.backend.features.auth.repository.BlacklistedTokenRepository;
import com.backend.security.jwt.JWTGenerator;
import com.backend.security.jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final JWTGenerator jwtGenerator;

    @Override
    public void blacklistToken(String token, String userIdentifier, String reason) {
        if (blacklistedTokenRepository.existsByToken(token)) {
            log.warn("Token already blacklisted: {}", userIdentifier);
            return;
        }

        try {
            Date expirationDate = jwtGenerator.getExpirationDateFromJWT(token);
            LocalDateTime expiryDateTime = convertToLocalDateTime(expirationDate);

            BlacklistedToken blacklistedToken = new BlacklistedToken(
                    token,
                    userIdentifier,
                    expiryDateTime,
                    reason
            );

            blacklistedTokenRepository.save(blacklistedToken);
            log.info("Token blacklisted: {} - Reason: {}", userIdentifier, reason);

        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage());
        }
    }

    @Override
    public void blacklistAllUserTokens(String userIdentifier, String reason) {
        try {
            blacklistedTokenRepository.deleteByUserIdentifier(userIdentifier);
            log.info("All tokens invalidated for user: {} - Reason: {}", userIdentifier, reason);
        } catch (Exception e) {
            log.error("Failed to blacklist all user tokens: {}", e.getMessage());
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    @Override
    public int cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = blacklistedTokenRepository.deleteExpiredTokens(now);
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired tokens", deletedCount);
            }
            
            return deletedCount;
        } catch (Exception e) {
            log.error("Failed to cleanup expired tokens: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BlacklistStats getBlacklistStats() {
        try {
            long totalTokens = blacklistedTokenRepository.count();
            long expiredTokens = blacklistedTokenRepository.countExpiredTokens(LocalDateTime.now());
            long activeTokens = totalTokens - expiredTokens;

            return new BlacklistStats(totalTokens, expiredTokens, activeTokens);
        } catch (Exception e) {
            log.error("Failed to get blacklist stats: {}", e.getMessage());
            return new BlacklistStats(0, 0, 0);
        }
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
