package com.backend.shared.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public abstract class BaseFilterRequest {

    private String search = "";

    @Min(value = 1, message = "Page number must be at least 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 101, message = "Page size cannot exceed 101")
    private Integer pageSize = 15;

    private String sortBy = "createdAt";

    private String sortDirection = "DESC";
}
