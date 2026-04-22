package com.backend.enums.user;

import lombok.Getter;

@Getter
public enum AccountStatus {
    ACTIVE("Active"),
    END_WORK("End Work"),
    LOCKED("Locked");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }
}