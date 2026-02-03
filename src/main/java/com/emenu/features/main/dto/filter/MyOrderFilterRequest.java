package com.emenu.features.main.dto.filter;

import com.emenu.enums.order.OrderStatus;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MyOrderFilterRequest extends BaseFilterRequest {
    private OrderStatus status;
}
