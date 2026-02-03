package com.emenu.features.main.dto.request;

import com.emenu.enums.payment.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentMethodCreateRequest {

    @NotNull(message = "Payment method type is required")
    private PaymentMethodType type;

    @NotBlank(message = "Payment method name is required")
    private String name;

    private String bankName;

    private String accountName;

    private String accountNumber;

    private String qrCodeImageUrl;

    private String description;

    private Boolean isActive = true;

    private Integer sortOrder = 0;
}
