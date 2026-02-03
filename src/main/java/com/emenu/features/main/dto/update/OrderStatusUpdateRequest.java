package com.emenu.features.main.dto.update;

import com.emenu.enums.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String rejectionReason;
}
