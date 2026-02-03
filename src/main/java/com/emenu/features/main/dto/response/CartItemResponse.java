package com.emenu.features.main.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class CartItemResponse extends BaseAuditResponse {
    private UUID productId;
    private String productName;
    private String productMainImageUrl;
    private UUID productSizeId;
    private String productSizeName;
    private Integer quantity;
    private BigDecimal originalPrice;
    private BigDecimal displayPrice;
    private BigDecimal unitPrice;
    private BigDecimal totalOriginalPrice;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private String promotionType;
    private BigDecimal promotionValue;
    private LocalDateTime promotionFromDate;
    private LocalDateTime promotionToDate;
    private Boolean hasActivePromotion;
    private String note;
}
