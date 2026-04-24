package com.backend.features.notification.repository;

import com.backend.features.notification.models.SmsConfig;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmsConfigRepository extends BaseRepository<SmsConfig, UUID> {

    @Query("SELECT c FROM SmsConfig c WHERE c.configType = :configType")
    Optional<SmsConfig> findByConfigType(@Param("configType") String configType);

    @Query("SELECT c FROM SmsConfig c WHERE c.configType = :configType AND c.isActive = true")
    Optional<SmsConfig> findActiveByConfigType(@Param("configType") String configType);
}
