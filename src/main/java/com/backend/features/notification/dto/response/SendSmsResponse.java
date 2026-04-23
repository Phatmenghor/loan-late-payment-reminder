package com.backend.features.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendSmsResponse {

    private String status;

    private String message;

    private String phoneNumber;

    private String smsStatus;
}
