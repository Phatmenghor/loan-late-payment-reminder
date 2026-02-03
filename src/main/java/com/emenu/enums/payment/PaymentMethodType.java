package com.emenu.enums.payment;

import lombok.Getter;

@Getter
public enum PaymentMethodType {
    CASH("Cash Payment"),
    BANK("Bank Transfer");

    private final String description;

    PaymentMethodType(String description) {
        this.description = description;
    }
}
