package com.backend.features.auth.model;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ad_user_cache",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ad_user_cache_username", columnNames = "username")
    },
    indexes = {
        @Index(name = "idx_ad_user_cache_username", columnList = "username")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AdUserCache extends BaseUUIDEntity {

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "encrypted_password", nullable = false, length = 255)
    private String encryptedPassword;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "cn", length = 255)
    private String cn;

    @Column(name = "given_name", length = 255)
    private String givenName;

    @Column(name = "sn", length = 255)
    private String sn;

    @Column(name = "mail", length = 255)
    private String mail;

    @Column(name = "department", length = 255)
    private String department;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "telephone_number", length = 100)
    private String telephoneNumber;

    @Column(name = "mobile", length = 100)
    private String mobile;

    @Column(name = "company", length = 255)
    private String company;

    @Column(name = "distinguished_name", columnDefinition = "TEXT")
    private String distinguishedName;

    @Column(name = "member_of", columnDefinition = "TEXT")
    private String memberOf;

    @Column(name = "ad_attributes", columnDefinition = "TEXT")
    private String adAttributes;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public void copyProfileFrom(com.backend.features.auth.dto.response.AdUserDto dto, String attrsJson) {
        if (dto != null) {
            if (dto.getDisplayName() != null) this.displayName = dto.getDisplayName();
            if (dto.getCn() != null) this.cn = dto.getCn();
            if (dto.getGivenName() != null) this.givenName = dto.getGivenName();
            if (dto.getSn() != null) this.sn = dto.getSn();
            if (dto.getMail() != null) this.mail = dto.getMail();
            if (dto.getDepartment() != null) this.department = dto.getDepartment();
            if (dto.getTitle() != null) this.title = dto.getTitle();
            if (dto.getTelephoneNumber() != null) this.telephoneNumber = dto.getTelephoneNumber();
            if (dto.getMobile() != null) this.mobile = dto.getMobile();
            if (dto.getCompany() != null) this.company = dto.getCompany();
            if (dto.getDistinguishedName() != null) this.distinguishedName = dto.getDistinguishedName();
        }
        if (attrsJson != null) {
            this.adAttributes = attrsJson;
        }
    }
}
