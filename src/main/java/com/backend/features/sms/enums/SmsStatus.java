package com.backend.features.sms.enums;

import lombok.Getter;

@Getter
public enum SmsStatus {
    PROCESSING("PROCESSING", "SMS is being sent"),
    SUCCESS("SUCCESS", "SMS sent successfully"),
    ERROR("ERROR", "SMS send failed");

    private final String value;
    private final String description;

    SmsStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static SmsStatus fromValue(String value) {
        for (SmsStatus status : SmsStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}
