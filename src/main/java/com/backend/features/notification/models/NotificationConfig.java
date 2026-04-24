package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_config", indexes = {
        @Index(name = "idx_notification_config_type", columnList = "config_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class NotificationConfig extends BaseUUIDEntity {

    @Column(name = "config_type", nullable = false, unique = true, length = 50)
    private String configType; // NOTIFICATION_LOAN_LATE

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue; // The SMS message content

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
