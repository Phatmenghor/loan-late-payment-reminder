package com.emenu.features.main.dto.response;

import com.emenu.enums.payment.PaymentMethodType;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentMethodResponse extends BaseAuditResponse {
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
