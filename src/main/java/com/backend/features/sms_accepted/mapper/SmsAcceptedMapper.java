package com.backend.features.sms_accepted.mapper;

import com.backend.features.sms_accepted.dto.SendAcceptedSmsResponse;
import com.backend.features.sms_accepted.models.SmsAcceptedLog;
import org.springframework.stereotype.Component;

@Component
public class SmsAcceptedMapper {

    public SendAcceptedSmsResponse toSendAcceptedSmsResponse(SmsAcceptedLog smsLog) {
        if (smsLog == null) {
            return null;
        }

        return SendAcceptedSmsResponse.builder()
                .id(smsLog.getId() != null ? smsLog.getId().toString() : null)
                .msgId(smsLog.getMsgId())
                .phone(smsLog.getPhone())
                .smsStatus(smsLog.getSmsStatus())
                .responseCode(smsLog.getResponseCode())
                .responseMessage(smsLog.getResponseMessage())
                .createdAt(smsLog.getCreatedAt())
                .sentAt(smsLog.getSentAt())
                .build();
    }
}
