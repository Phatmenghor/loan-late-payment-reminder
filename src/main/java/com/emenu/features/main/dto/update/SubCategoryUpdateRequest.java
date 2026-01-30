package com.emenu.features.main.dto.update;

import com.emenu.enums.common.Status;
import lombok.Data;

import java.util.UUID;

@Data
public class SubCategoryUpdateRequest {
    private String name;
    private UUID categoryId;
    private String imageUrl;
    private Status status;
}
