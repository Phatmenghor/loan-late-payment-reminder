package com.backend.features.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OracleSmsDto {

    private String msgId;
    private String phone;
    private String smsStatus;
}
