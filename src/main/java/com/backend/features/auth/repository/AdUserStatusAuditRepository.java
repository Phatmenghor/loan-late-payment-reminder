package com.backend.features.auth.repository;

import com.backend.features.auth.model.AdUserStatusAudit;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdUserStatusAuditRepository extends BaseRepository<AdUserStatusAudit, UUID> {

    List<AdUserStatusAudit> findByApiKeyAndUsernameOrderByCreatedAtDesc(String apiKey, String username);
}
