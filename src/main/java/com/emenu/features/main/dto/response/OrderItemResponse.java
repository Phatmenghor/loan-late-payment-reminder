package com.emenu.features.main.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderItemResponse extends BaseAuditResponse {
    private UUID productId;
    private UUID productSizeId;
    private String productName;
    private String productNameKh;
    private String sizeName;
    private String sizeNameKh;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private String note;
    private String productImageUrl;
}
