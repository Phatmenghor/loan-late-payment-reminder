package com.emenu.features.main.models;

import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_id", "product_size_id"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CartItem extends BaseUUIDEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @Column(name = "product_size_id")
    private UUID productSizeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_id", insertable = false, updatable = false)
    private ProductSize productSize;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "original_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "note")
    private String note;

    public void incrementQuantity(int amount) {
        this.quantity = this.quantity + amount;
    }

    public void decrementQuantity(int amount) {
        this.quantity = Math.max(1, this.quantity - amount);
    }

    public BigDecimal getTotalPrice() {
        return originalPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
