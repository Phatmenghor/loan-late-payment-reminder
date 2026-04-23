package com.backend.features.notification.repository;

import com.backend.features.notification.models.Setting;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingRepository extends BaseRepository<Setting, Long> {

    Optional<Setting> findByKey(String key);
}
