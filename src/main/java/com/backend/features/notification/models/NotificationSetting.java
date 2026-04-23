package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "loan_notification_setting",
    uniqueConstraints = @UniqueConstraint(columnNames = "setting_key", name = "uq_setting_key"),
    indexes = {
        @Index(name = "idx_setting_key", columnList = "setting_key"),
        @Index(name = "idx_setting_is_deleted", columnList = "is_deleted")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationSetting extends BaseUUIDEntity {

    @Column(name = "setting_key", nullable = false, length = 255)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "setting_description", columnDefinition = "TEXT")
    private String description;

    @Override
    public String toString() {
        return String.format(
            "NotificationSetting(id=%s, key=%s, value=%s, description=%s)",
            getId(), key, value, description != null ? description.substring(0, Math.min(50, description.length())) + "..." : null
        );
    }
}
