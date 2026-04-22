package com.backend.features.auth.dto.update;

import lombok.Data;

@Data
public class BusinessHoursUpdateRequest {

    private String day;

    private String openingTime;

    private String closingTime;
}
