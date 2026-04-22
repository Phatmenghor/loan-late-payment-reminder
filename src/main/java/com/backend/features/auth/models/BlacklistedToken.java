package com.backend.features.auth.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken extends BaseUUIDEntity {

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "user_identifier", nullable = false)
    private String userIdentifier;

    @Column(name = "blacklisted_at", nullable = false)
    private LocalDateTime blacklistedAt;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "reason")
    private String reason;

    public BlacklistedToken(String token, String userIdentifier, LocalDateTime expiryDate, String reason) {
        this.token = token;
        this.userIdentifier = userIdentifier;
        this.blacklistedAt = LocalDateTime.now();
        this.expiryDate = expiryDate;
        this.reason = reason;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}