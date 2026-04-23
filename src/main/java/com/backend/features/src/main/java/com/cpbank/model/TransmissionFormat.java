package com.backend.features.src.main.java.com.cpbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransmissionFormat {
    @JsonProperty("phone")
    private String phone;

    @JsonProperty("content")
    private String content;

    @JsonProperty("signKey")
    private String signKey;
}
