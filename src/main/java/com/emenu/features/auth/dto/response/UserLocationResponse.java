package com.emenu.features.auth.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserLocationResponse extends BaseAuditResponse {
    private UUID userId;
    private String label;
    private Double latitude;
    private Double longitude;
    private String houseNumber;
    private String streetNumber;
    private String village;
    private String commune;
    private String district;
    private String province;
    private String country;
    private String note;
    private Boolean isPrimary;
}
