package com.emenu.features.main.dto.response;

import com.emenu.enums.product.ProductStatus;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductDetailDto extends BaseAuditResponse {
    private String name;
    private String description;
    private ProductStatus status;

    private BigDecimal price;
    private String promotionType;
    private BigDecimal promotionValue;
    private LocalDateTime promotionFromDate;
    private LocalDateTime promotionToDate;

    private BigDecimal displayPrice;
    private BigDecimal displayOriginPrice;
    private String displayPromotionType;
    private BigDecimal displayPromotionValue;
    private LocalDateTime displayPromotionFromDate;
    private LocalDateTime displayPromotionToDate;

    private Boolean hasSizes;
    private Boolean hasActivePromotion;
    private String mainImageUrl;
    private Long viewCount;
    private Long favoriteCount;
    private Boolean isFavorited;

    private Integer quantityInCart;

    private UUID subCategoryId;
    private String subCategoryName;
    private UUID categoryId;
    private String categoryName;

    private List<ProductImageDto> images;
    private List<ProductSizeDto> sizes;
}