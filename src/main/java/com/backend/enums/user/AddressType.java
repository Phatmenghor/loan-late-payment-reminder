package com.backend.enums.user;

import lombok.Getter;

@Getter
public enum AddressType {
    CURRENT("Current Address"),
    PLACE_OF_BIRTH("Place of Birth");

    private final String description;
    AddressType(String d) { this.description = d; }
}
