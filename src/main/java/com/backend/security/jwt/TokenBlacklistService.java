package com.backend.security.jwt;

public interface TokenBlacklistService {

    void blacklistToken(String token, String userIdentifier, String reason);
    
    void blacklistAllUserTokens(String userIdentifier, String reason);
    
    boolean isTokenBlacklisted(String token);
    
    int cleanupExpiredTokens();
    
    BlacklistStats getBlacklistStats();

    class BlacklistStats {
        public final long totalTokens;
        public final long expiredTokens;
        public final long activeTokens;

        public BlacklistStats(long totalTokens, long expiredTokens, long activeTokens) {
            this.totalTokens = totalTokens;
            this.expiredTokens = expiredTokens;
            this.activeTokens = activeTokens;
        }

        @Override
        public String toString() {
            return String.format("Total: %d, Expired: %d, Active: %d", 
                    totalTokens, expiredTokens, activeTokens);
        }
    }
}