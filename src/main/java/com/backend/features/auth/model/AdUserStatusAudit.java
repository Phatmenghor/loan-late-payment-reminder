package com.backend.features.auth.model;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "ad_user_status_audits",
    indexes = {
        @Index(name = "idx_ad_user_status_audits_api_key_username", columnList = "api_key, username"),
        @Index(name = "idx_ad_user_status_audits_action", columnList = "action"),
        @Index(name = "idx_ad_user_status_audits_created_at", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AdUserStatusAudit extends BaseUUIDEntity {

    @Column(name = "api_key", nullable = false, length = 255)
    private String apiKey;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // "LOCKED" or "UNLOCKED"

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}
