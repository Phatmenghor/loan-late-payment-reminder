package com.backend.features.auth.model;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "ad_config",
    indexes = {
        @Index(name = "idx_ad_config_created_at", columnList = "created_at"),
        @Index(name = "idx_ad_config_ad_enabled", columnList = "ad_enabled")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AdConfig extends BaseUUIDEntity {

    @Column(name = "ad_enabled", nullable = false)
    @Builder.Default
    private Boolean adEnabled = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
