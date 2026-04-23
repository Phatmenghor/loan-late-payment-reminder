package com.backend.features.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendSmsRequestDto {
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Message content is required")
    private String messageContent;
}
