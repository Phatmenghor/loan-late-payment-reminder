package com.emenu.features.main.dto.request;

import com.emenu.enums.product.ProductStatus;
import com.emenu.enums.product.PromotionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProductCreateDto {
    
    @NotBlank(message = "Product name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Category is required")
    private UUID categoryId;

    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;
    
    private String mainImageUrl;
    
    private PromotionType promotionType;
    private BigDecimal promotionValue;
    private LocalDateTime promotionFromDate;
    private LocalDateTime promotionToDate;
    
    @Valid
    private List<ProductImageCreateDto> images;
    
    @Valid
    private List<ProductSizeCreateDto> sizes;
    
    private ProductStatus status = ProductStatus.ACTIVE;
}