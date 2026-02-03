package com.emenu.features.main.dto.response;

import com.emenu.enums.order.OrderStatus;
import com.emenu.features.auth.dto.response.UserLocationResponse;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderResponse extends BaseAuditResponse {
    private String orderNumber;
    private UUID userId;
    private String customerName;
    private String customerPhone;
    private OrderStatus status;
    private String statusDescription;
    private Integer totalItems;
    private BigDecimal totalOriginalPrice;
    private BigDecimal totalDiscount;
    private BigDecimal totalPayment;
    private String note;
    private String rejectionReason;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private LocalDateTime completedAt;
    private PaymentMethodResponse paymentMethod;
    private UserLocationResponse location;
    private List<OrderItemResponse> orderItems;
}
