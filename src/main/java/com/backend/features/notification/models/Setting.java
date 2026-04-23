package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "d_cbs_setting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Setting extends BaseUUIDEntity {

    @Column(name = "set_desc", columnDefinition = "TEXT")
    private String description;

    @Column(name = "set_key")
    private String key;

    @Column(name = "set_value")
    private String value;
}
