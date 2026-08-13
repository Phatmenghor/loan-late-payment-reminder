package com.backend.features.auth.model;

import com.backend.enums.common.Status;
import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "ad_api_keys",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ad_api_keys_label", columnNames = "label")
    },
    indexes = {
        @Index(name = "idx_ad_api_keys_api_key", columnList = "api_key"),
        @Index(name = "idx_ad_api_keys_label", columnList = "label"),
        @Index(name = "idx_ad_api_keys_status", columnList = "status"),
        @Index(name = "idx_ad_api_keys_is_deleted", columnList = "is_deleted"),
        @Index(name = "idx_ad_api_keys_lookup", columnList = "api_key, is_deleted, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AdApiKey extends BaseUUIDEntity {

    @Column(name = "api_key", nullable = false, unique = true, length = 255)
    private String apiKey;

    @Column(name = "label", nullable = false, unique = true, length = 255)
    private String label;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private Status status = Status.ACTIVE;
}
