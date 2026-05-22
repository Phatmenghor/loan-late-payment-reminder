package com.backend.features.auth.model;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ad_config")
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
