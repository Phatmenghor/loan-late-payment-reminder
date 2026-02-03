package com.emenu.features.main.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CheckoutRequest {

    @NotNull(message = "Payment method ID is required")
    private UUID paymentMethodId;

    @NotNull(message = "Location ID is required")
    private UUID locationId;

    private String note;
}
