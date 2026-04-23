package com.backend.features.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusResponseDto {

    @JsonProperty("ERROR_CODE")
    private Integer errorCode;

    @JsonProperty("ERROR_MESSAGE")
    private String errorMessage;
}
