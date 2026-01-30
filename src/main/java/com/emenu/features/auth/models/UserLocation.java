package com.emenu.features.auth.models;

import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_locations")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserLocation extends BaseUUIDEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "label")
    private String label;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "house_number")
    private String houseNumber;

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "village")
    private String village;

    @Column(name = "commune")
    private String commune;

    @Column(name = "district")
    private String district;

    @Column(name = "province")
    private String province;

    @Column(name = "country")
    private String country;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    public void setPrimary() {
        this.isPrimary = true;
    }

    public void unsetPrimary() {
        this.isPrimary = false;
    }
}
