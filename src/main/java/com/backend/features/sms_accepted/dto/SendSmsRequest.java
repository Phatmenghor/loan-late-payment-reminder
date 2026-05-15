package com.backend.features.sms_accepted.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendSmsRequest {

    @NotBlank(message = "Phone number is required")
    @JsonProperty("phone")
    private String phone;

    @NotBlank(message = "SMS content is required")
    @JsonProperty("content")
    private String content;
}
