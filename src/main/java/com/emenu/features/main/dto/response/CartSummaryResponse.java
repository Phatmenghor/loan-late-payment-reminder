package com.emenu.features.main.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartSummaryResponse {
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalOriginalPrice;
    private BigDecimal totalDiscount;
    private BigDecimal totalPayment;
}
