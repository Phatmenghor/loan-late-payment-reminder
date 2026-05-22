package com.backend.features.auth.repository;

import com.backend.features.auth.model.AdConfig;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdConfigRepository extends BaseRepository<AdConfig, UUID> {

    Optional<AdConfig> findFirstByOrderByCreatedAtAsc();
}
