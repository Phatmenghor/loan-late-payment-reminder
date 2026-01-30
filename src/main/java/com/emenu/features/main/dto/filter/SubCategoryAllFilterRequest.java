package com.emenu.features.main.dto.filter;

import com.emenu.enums.common.Status;
import com.emenu.shared.dto.BaseAllFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class SubCategoryAllFilterRequest extends BaseAllFilterRequest {
    private Status status;
    private UUID categoryId;
}
