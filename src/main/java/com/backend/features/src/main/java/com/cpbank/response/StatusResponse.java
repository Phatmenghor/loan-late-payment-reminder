package com.backend.features.src.main.java.com.cpbank.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StatusResponse {
    @JsonProperty("ERROR_CODE")
    private Integer ERROR_CODE;

    @JsonProperty("ERROR_MESSAGE")
    private String ERROR_MESSAGE;
}