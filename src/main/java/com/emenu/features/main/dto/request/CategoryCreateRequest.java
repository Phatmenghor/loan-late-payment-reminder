package com.emenu.features.main.dto.request;

import com.emenu.enums.common.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    
    @NotBlank(message = "Category name is required")
    private String name;

    private String imageUrl;
    private Status status = Status.ACTIVE;
}