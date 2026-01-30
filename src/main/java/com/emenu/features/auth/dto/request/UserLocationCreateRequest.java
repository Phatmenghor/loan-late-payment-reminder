package com.emenu.features.auth.dto.request;

import lombok.Data;

@Data
public class UserLocationCreateRequest {
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
