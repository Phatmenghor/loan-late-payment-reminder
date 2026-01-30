package com.emenu.features.main.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CartItemCreateRequest {

    @NotNull(message = "Product is required")
    private UUID productId;

    private UUID productSizeId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be 0 or more")
    private Integer quantity = 1;

    private String note;
}
