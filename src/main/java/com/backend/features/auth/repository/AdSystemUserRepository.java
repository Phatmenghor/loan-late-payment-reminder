package com.backend.features.auth.repository;

import com.backend.enums.common.AdUserStatus;
import com.backend.features.auth.model.AdSystemUser;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdSystemUserRepository extends BaseRepository<AdSystemUser, UUID> {

    Optional<AdSystemUser> findByApiKeyAndUsername(String apiKey, String username);

    @Query("SELECT u FROM AdSystemUser u WHERE u.status = :status " +
           "AND ((u.lastLoginAt IS NOT NULL AND u.lastLoginAt < :cutoffDate) " +
           "     OR (u.lastLoginAt IS NULL AND u.createdAt < :cutoffDate))")
    List<AdSystemUser> findInactiveUsers(
            @Param("status") AdUserStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );
}
