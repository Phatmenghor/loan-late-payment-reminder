package com.backend.enums.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AdUserStatus {
    ACTIVE("ACTIVE"),
    LOCKED("LOCKED"),
    DELETED("DELETED");

    private final String value;

    AdUserStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isLocked() {
        return this == LOCKED;
    }

    public boolean isDeletedStatus() {
        return this == DELETED;
    }

    @JsonCreator
    public static AdUserStatus fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().toUpperCase();
        if ("LOCK".equals(normalized) || "LOCKED".equals(normalized) || "INACTIVE".equals(normalized)) {
            return LOCKED;
        }
        if ("ACTIVE".equals(normalized) || "UNLOCK".equals(normalized) || "UNLOCKED".equals(normalized)) {
            return ACTIVE;
        }
        if ("DELETE".equals(normalized) || "DELETED".equals(normalized)) {
            return DELETED;
        }
        for (AdUserStatus b : AdUserStatus.values()) {
            if (b.name().equalsIgnoreCase(normalized)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown user status: " + text + ". Expected ACTIVE, LOCKED (or LOCK), DELETED");
    }
}
