package com.backend.features.auth.repository;

import com.backend.features.auth.model.AdApiKey;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdApiKeyRepository extends BaseRepository<AdApiKey, UUID> {

    Optional<AdApiKey> findByApiKeyAndIsDeletedFalse(String apiKey);

    boolean existsByLabelAndIsDeletedFalse(String label);

    List<AdApiKey> findByIsDeletedFalse();
}
