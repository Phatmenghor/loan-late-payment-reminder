package com.backend.features.auth.repository;

import com.backend.features.auth.model.AdLog;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdLogRepository extends BaseRepository<AdLog, UUID> {
}
