package com.emenu.enums.order;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("Order placed, awaiting confirmation"),
    PREPARING("Order is being prepared"),
    ON_DELIVERY("Order is out for delivery"),
    SUCCESS("Order completed successfully"),
    CANCELLED("Order cancelled by user"),
    REJECTED("Order rejected by admin");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public boolean canCancel() {
        return this == PENDING;
    }

    public boolean canUpdateByAdmin() {
        return this == PENDING || this == PREPARING || this == ON_DELIVERY;
    }

    public boolean isCompleted() {
        return this == SUCCESS || this == CANCELLED || this == REJECTED;
    }
}
