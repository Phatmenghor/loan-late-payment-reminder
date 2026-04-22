package com.backend.enums.user;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("Admin", "Full system access - platform administrator"),
    STAFF("Staff", "Staff member access - can manage orders and products"),
    CUSTOMER("Customer", "Customer access - can browse and purchase products");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
