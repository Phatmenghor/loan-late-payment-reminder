package com.backend.features.auth.repository;

import com.backend.features.auth.model.AdUserCache;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdUserCacheRepository extends BaseRepository<AdUserCache, UUID> {

    Optional<AdUserCache> findByUsernameAndIsDeletedFalse(String username);
}
