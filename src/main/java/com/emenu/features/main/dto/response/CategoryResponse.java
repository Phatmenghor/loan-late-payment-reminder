package com.emenu.features.main.dto.response;

import com.emenu.enums.common.Status;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CategoryResponse extends BaseAuditResponse {
    private String name;
    private String imageUrl;
    private Status status;
}