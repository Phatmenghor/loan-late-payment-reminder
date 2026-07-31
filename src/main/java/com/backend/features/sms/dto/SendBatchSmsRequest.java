package com.backend.features.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendBatchSmsRequest {

    /**
     * Custom SMS message content to send for this batch.
     * If provided, overrides default database configuration.
     */
    private String customMessage;

    /**
     * Optional explicit list of phone numbers to process.
     * If provided with customMessage, sends customMessage to all these phone numbers.
     */
    private List<String> phoneNumbers;

    /**
     * Optional explicit list of individual SMS items (phone & custom content).
     */
    private List<SmsItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsItemRequest {
        private String phone;
        private String content;
    }
}
