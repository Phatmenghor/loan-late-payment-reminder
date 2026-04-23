package com.backend.features.notification.models;

import com.backend.shared.models.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "D_CBS_SETTING")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Setting extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_desc", columnDefinition = "TEXT")
    private String description;

    @Column(name = "set_key")
    private String key;

    @Column(name = "set_value")
    private String value;
}
