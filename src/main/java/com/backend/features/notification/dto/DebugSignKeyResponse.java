package com.backend.features.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebugSignKeyResponse {
    private String phone;
    private String signKey;
    private String signKeyFormula;
    private String payload;
    private String apiUrl;
    private String apiResponse;
    private String apiCode;
    private String apiDesc;
}
