package com.backend.features.auth.repository;

import com.backend.features.auth.models.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, UUID> {

    /**
     * Finds a blacklisted token by its token string
     */
    Optional<BlacklistedToken> findByToken(String token);

    /**
     * Checks if a token exists in the blacklist
     */
    boolean existsByToken(String token);

    /**
     * Checks if any token exists for a given user identifier
     */
    @Query("SELECT COUNT(bt) > 0 FROM BlacklistedToken bt WHERE bt.userIdentifier = :userIdentifier")
    boolean existsByUserIdentifier(@Param("userIdentifier") String userIdentifier);

    /**
     * Deletes all blacklisted tokens that have expired before the given time
     */
    @Modifying
    @Query("DELETE FROM BlacklistedToken bt WHERE bt.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Deletes all blacklisted tokens for a specific user identifier
     */
    @Modifying
    @Query("DELETE FROM BlacklistedToken bt WHERE bt.userIdentifier = :userIdentifier")
    void deleteByUserIdentifier(@Param("userIdentifier") String userIdentifier);

    /**
     * Counts the number of expired tokens before the given time
     */
    @Query("SELECT COUNT(bt) FROM BlacklistedToken bt WHERE bt.expiryDate < :now")
    long countExpiredTokens(@Param("now") LocalDateTime now);
}