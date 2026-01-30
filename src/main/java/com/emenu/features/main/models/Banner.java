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
@Table(name = "banners")
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

    public boolean isActive() {
        return Status.ACTIVE.equals(status);
    }
}