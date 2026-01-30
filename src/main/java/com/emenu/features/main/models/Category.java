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
@Table(name = "categories", indexes = {
        // ✅ ESSENTIAL INDEXES ONLY for Category

        // 1. Business categories (most common in product filtering)
        @Index(name = "idx_categories_business_deleted",
                columnList = "business_id, is_deleted"),

        // 2. Business categories by status
        @Index(name = "idx_categories_business_status_deleted",
                columnList = "business_id, status, is_deleted"),

        // 3. Base soft delete index
        @Index(name = "idx_categories_deleted",
                columnList = "is_deleted")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseUUIDEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

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