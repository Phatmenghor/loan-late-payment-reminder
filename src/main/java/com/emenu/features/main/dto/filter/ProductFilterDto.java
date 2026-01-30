package com.emenu.features.main.dto.filter;

import com.emenu.enums.product.ProductStatus;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductFilterDto extends BaseFilterRequest {
    private UUID subCategoryId;
    private UUID categoryId;
    private ProductStatus status;
    private Boolean hasPromotion;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
