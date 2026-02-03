package com.emenu.features.main.dto.filter;

import com.emenu.enums.payment.PaymentMethodType;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentMethodFilterRequest extends BaseFilterRequest {
    private PaymentMethodType type;
    private Boolean isActive;
}
