package com.emenu.features.main.models;

import com.emenu.enums.common.Status;
import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "banners", indexes = {
        // ✅ FIXED: BaseUUIDEntity indexes
        @Index(name = "idx_banner_deleted", columnList = "is_deleted"),
        @Index(name = "idx_banner_deleted_created", columnList = "is_deleted, created_at"),

        // ✅ FIXED: Business relationship indexes
        @Index(name = "idx_banner_business_deleted", columnList = "business_id, is_deleted"),
        @Index(name = "idx_banner_status_deleted", columnList = "status, is_deleted"),
        @Index(name = "idx_banner_business_status_deleted", columnList = "business_id, status, is_deleted")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Banner extends BaseUUIDEntity {

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "link_url")
    private String linkUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    // Business Methods
    public void activate() {
        this.status = Status.ACTIVE;
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
    }

    public boolean isActive() {
        return Status.ACTIVE.equals(status);
    }
}