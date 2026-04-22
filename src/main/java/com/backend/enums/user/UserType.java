package com.backend.enums.user;

import lombok.Getter;

@Getter
public enum UserType {
    OWNER("Owner"),
    CUSTOMER("Customer");

    private final String description;

    UserType(String description) {
        this.description = description;
    }

}