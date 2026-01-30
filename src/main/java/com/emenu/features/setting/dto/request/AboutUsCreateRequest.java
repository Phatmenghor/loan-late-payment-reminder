package com.emenu.features.setting.dto.request;

import com.emenu.enums.common.Status;
import lombok.Data;

@Data
public class AboutUsCreateRequest {
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
    private Status status = Status.ACTIVE;
}
