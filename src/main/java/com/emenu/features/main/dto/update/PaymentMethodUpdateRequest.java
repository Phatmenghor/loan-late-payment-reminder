package com.emenu.features.main.dto.update;

import com.emenu.enums.payment.PaymentMethodType;
import lombok.Data;

@Data
public class PaymentMethodUpdateRequest {

    private PaymentMethodType type;

    private String name;

    private String bankName;

    private String accountName;

    private String accountNumber;

    private String qrCodeImageUrl;

    private String description;

    private Boolean isActive;

    private Integer sortOrder;
}
