package com.emenu.features.main.dto.response;

import com.emenu.enums.common.Status;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class SubCategoryResponse extends BaseAuditResponse {
    private String name;
    private String imageUrl;
    private Status status;
    private UUID categoryId;
    private String categoryName;
}
