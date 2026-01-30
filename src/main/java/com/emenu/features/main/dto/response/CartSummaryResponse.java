package com.emenu.features.main.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartSummaryResponse {
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalPrice;
}
