package com.backend.features.notification.repository;

import com.backend.features.notification.models.NotificationSetting;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationSettingRepository extends BaseRepository<NotificationSetting, UUID> {

    @Query("SELECT s FROM NotificationSetting s WHERE s.key = :key AND s.isDeleted = false")
    Optional<NotificationSetting> findByKey(@Param("key") String key);

    @Query("SELECT s FROM NotificationSetting s WHERE s.isDeleted = false")
    List<NotificationSetting> findAllActive();
}
