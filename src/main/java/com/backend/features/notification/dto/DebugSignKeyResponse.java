package com.backend.features.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebugSignKeyResponse {
    private String phone;
    private String content;
    private String apiUrl;
    private Map<String, String> signKeyVariants;  // formula -> hash
    private String currentFormula;
    private String currentSignKey;
    private String payload;
    private String apiResponse;
}
