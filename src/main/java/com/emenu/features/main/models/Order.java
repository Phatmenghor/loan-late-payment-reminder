package com.emenu.features.main.models;

import com.emenu.enums.order.OrderStatus;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.models.UserLocation;
import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseUUIDEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", insertable = false, updatable = false)
    private UserLocation location;

    @Column(name = "payment_method_id", nullable = false)
    private UUID paymentMethodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", insertable = false, updatable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_items", nullable = false)
    private Integer totalItems;

    @Column(name = "total_original_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalOriginalPrice;

    @Column(name = "total_discount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "total_payment", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPayment;

    @Column(name = "note")
    private String note;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    public boolean canCancel() {
        return status.canCancel();
    }

    public boolean canUpdateByAdmin() {
        return status.canUpdateByAdmin();
    }

    public void cancel(String user) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = user;
    }

    public void reject(String user, String reason) {
        this.status = OrderStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectedBy = user;
        this.rejectionReason = reason;
    }

    public void complete() {
        this.status = OrderStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }
}
