package com.emenu.features.setting.dto.response;

import com.emenu.enums.common.Status;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AboutUsResponse extends BaseAuditResponse {
    private String title;
    private String description;
    private String mainImageUrl;
    private String contactInfo;
    private String showroomHours;
    private String phoneNumber;
    private String email;
    private String address;
    private String facebookUrl;
    private String instagramUrl;
    private String websiteUrl;
    private Double latitude;
    private Double longitude;
    private Status status;
}
