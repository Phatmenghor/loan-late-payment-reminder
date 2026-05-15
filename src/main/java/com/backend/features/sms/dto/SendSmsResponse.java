package com.backend.features.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendSmsResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("status")
    private String status;

    @JsonProperty("sent_at")
    private LocalDateTime sentAt;

    @JsonProperty("error")
    private String error;
}
