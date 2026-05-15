package com.backend.features.sms_accepted.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendAcceptedSmsRequest {

    private String phone;
    private String message;
    private String msgId;
}
