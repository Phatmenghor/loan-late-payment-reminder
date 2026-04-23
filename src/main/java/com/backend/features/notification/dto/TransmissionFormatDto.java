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
public class TransmissionFormatDto {

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("content")
    private String content;

    @JsonProperty("signKey")
    private String signKey;
}
