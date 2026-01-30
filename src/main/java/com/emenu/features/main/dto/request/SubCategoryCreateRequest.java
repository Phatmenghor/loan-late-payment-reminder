package com.emenu.features.main.dto.request;

import com.emenu.enums.common.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SubCategoryCreateRequest {

    @NotBlank(message = "SubCategory name is required")
    private String name;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    private String imageUrl;
    private Status status = Status.ACTIVE;
}
