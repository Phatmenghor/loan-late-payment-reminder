package com.emenu.features.main.mapper;

import com.emenu.enums.product.PromotionType;
import com.emenu.features.main.dto.request.ProductCreateDto;
import com.emenu.features.main.dto.response.ProductDetailDto;
import com.emenu.features.main.dto.response.ProductListDto;
import com.emenu.features.main.dto.update.ProductUpdateDto;
import com.emenu.features.main.models.Product;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {ProductImageMapper.class, ProductSizeMapper.class, PaginationMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "viewCount", constant = "0L")
    @Mapping(target = "favoriteCount", constant = "0L")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "sizes", ignore = true)
    @Mapping(source = "promotionType", target = "promotionType", qualifiedByName = "stringToPromotionType")
    Product toEntity(ProductCreateDto dto);

    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "favoriteCount", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "sizes", ignore = true)
    @Mapping(source = "promotionType", target = "promotionType", qualifiedByName = "stringToPromotionType")
    @AfterMapping
    default void afterUpdate(ProductUpdateDto dto, @MappingTarget Product entity) {
        if (!dto.hasPromotionData()) {
            entity.setPromotionType(null);
            entity.setPromotionValue(null);
            entity.setPromotionFromDate(null);
            entity.setPromotionToDate(null);
        }
    }

    void updateEntity(ProductUpdateDto dto, @MappingTarget Product entity);

    @Mapping(source = "subCategory.categoryId", target = "categoryId")
    @Mapping(source = "displayPromotionType", target = "displayPromotionType", qualifiedByName = "promotionTypeToString")
    @Mapping(target = "isFavorited", constant = "false")
    ProductListDto toListDto(Product product);

    List<ProductListDto> toListDtos(List<Product> products);

    @Mapping(source = "subCategory.name", target = "subCategoryName")
    @Mapping(source = "subCategory.categoryId", target = "categoryId")
    @Mapping(source = "subCategory.category.name", target = "categoryName")
    @Mapping(source = "promotionType", target = "promotionType", qualifiedByName = "promotionTypeToString")
    @Mapping(source = "displayPromotionType", target = "displayPromotionType", qualifiedByName = "promotionTypeToString")
    @Mapping(target = "hasPromotion", source = "hasActivePromotion")
    @Mapping(target = "isFavorited", constant = "false")
    ProductDetailDto toDetailDto(Product product);

    @Named("stringToPromotionType")
    default PromotionType stringToPromotionType(String promotionType) {
        if (promotionType == null || promotionType.trim().isEmpty()) {
            return null;
        }
        try {
            return PromotionType.valueOf(promotionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("promotionTypeToString")
    default String promotionTypeToString(PromotionType promotionType) {
        return promotionType != null ? promotionType.name() : null;
    }

    default PaginationResponse<ProductListDto> toPaginationResponse(Page<Product> page, PaginationMapper paginationMapper) {
        return paginationMapper.toPaginationResponse(page, this::toListDtos);
    }
}
