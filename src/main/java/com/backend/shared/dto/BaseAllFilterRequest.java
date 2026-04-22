package com.backend.shared.dto;

import lombok.Data;

@Data
public abstract class BaseAllFilterRequest {
    private String search;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}
