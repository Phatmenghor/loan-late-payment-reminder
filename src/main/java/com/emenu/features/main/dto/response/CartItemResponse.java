package com.emenu.features.main.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
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
    private BigDecimal totalPrice;
    private String note;
}
