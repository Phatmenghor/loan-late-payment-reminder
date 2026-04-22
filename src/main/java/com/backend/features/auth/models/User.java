package com.backend.features.auth.models;

import com.backend.enums.user.AccountStatus;
import com.backend.enums.user.UserRole;
import com.backend.enums.user.UserType;
import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_identifier", columnNames = {"user_identifier"})
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"profile"})
@ToString(exclude = {"profile"})
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseUUIDEntity {

    // ── Core ──────────────────────────────────────────────────────────────────

    @Column(name = "user_identifier", nullable = false)
    private String userIdentifier;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;


    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    // ── Profile (separate table) ───────────────────────────────────────────

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getFullName() {
        if (profile != null) {
            String name = profile.getFullName();
            if (name != null) return name;
        }
        return userIdentifier;
    }

    public boolean isActive() { return AccountStatus.ACTIVE.equals(accountStatus); }
    public boolean isOwner() { return UserType.OWNER.equals(userType); }
    public boolean isCustomer() { return UserType.CUSTOMER.equals(userType); }

    public boolean isAdmin() { return UserRole.ADMIN.equals(userRole); }
    public boolean isStaff() { return UserRole.STAFF.equals(userRole); }
    public boolean hasAdminAccess() { return isAdmin() || isStaff(); }
}
