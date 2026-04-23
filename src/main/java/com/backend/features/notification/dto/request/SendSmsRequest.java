package com.backend.features.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^\\d{10,20}$", message = "Phone number must be 10-20 digits")
    private String phoneNumber;

    @NotBlank(message = "Message content is required")
    private String messageContent;
}
