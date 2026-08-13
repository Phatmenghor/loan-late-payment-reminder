package com.backend.features.auth.model;

import com.backend.enums.common.AdUserStatus;
import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ad_system_users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ad_system_users_api_key_username", columnNames = {"api_key", "username"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AdSystemUser extends BaseUUIDEntity {

    @Column(name = "api_key", nullable = false, length = 255)
    private String apiKey;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private AdUserStatus status = AdUserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "lock_reason", length = 255)
    private String lockReason;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    public void lock(String reason) {
        this.status = AdUserStatus.LOCKED;
        this.lockReason = reason;
        this.lockedAt = LocalDateTime.now();
    }

    public void unlock() {
        this.status = AdUserStatus.ACTIVE;
        this.lockReason = null;
        this.lockedAt = null;
        this.lastLoginAt = LocalDateTime.now();
    }
}
