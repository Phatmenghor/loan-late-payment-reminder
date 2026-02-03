package com.emenu.features.main.dto.filter;

import com.emenu.enums.order.OrderStatus;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderFilterRequest extends BaseFilterRequest {
    private OrderStatus status;
    private UUID userId;
    private UUID paymentMethodId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
