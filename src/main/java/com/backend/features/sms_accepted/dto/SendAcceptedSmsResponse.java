package com.backend.features.sms_accepted.dto;

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
public class SendAcceptedSmsResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("msg_id")
    private String msgId;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("sms_status")
    private String smsStatus;

    @JsonProperty("response_code")
    private String responseCode;

    @JsonProperty("response_message")
    private String responseMessage;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("sent_at")
    private LocalDateTime sentAt;
}
