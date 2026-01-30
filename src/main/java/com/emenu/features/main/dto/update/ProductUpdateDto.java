package com.emenu.features.main.dto.update;

import com.emenu.enums.product.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProductUpdateDto {
    
    private String name;
    private String description;
    private UUID categoryId;

    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;
    
    private String mainImageUrl;
    
    private String promotionType;
    private BigDecimal promotionValue;
    private LocalDateTime promotionFromDate;
    private LocalDateTime promotionToDate;
    
    @Valid
    private List<ProductImageUpdateDto> images;
    
    @Valid
    private List<ProductSizeUpdateDto> sizes;
    
    private ProductStatus status;
    
    public boolean hasPromotionData() {
        return promotionType != null && promotionValue != null;
    }
    
    public void clearPromotion() {
        this.promotionType = null;
        this.promotionValue = null;
        this.promotionFromDate = null;
        this.promotionToDate = null;
    }
}