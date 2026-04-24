package com.backend.features.notification.repository;

import com.backend.features.notification.models.NotificationConfig;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationConfigRepository extends BaseRepository<NotificationConfig, UUID> {

    @Query("SELECT c FROM NotificationConfig c WHERE c.configType = :configType")
    Optional<NotificationConfig> findByConfigType(@Param("configType") String configType);

    @Query("SELECT c FROM NotificationConfig c WHERE c.configType = :configType AND c.isActive = true")
    Optional<NotificationConfig> findActiveByConfigType(@Param("configType") String configType);
}
