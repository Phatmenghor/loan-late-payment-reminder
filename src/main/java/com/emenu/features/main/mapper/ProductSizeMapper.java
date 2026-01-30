package com.emenu.features.main.mapper;

import com.emenu.enums.product.PromotionType;
import com.emenu.features.main.dto.request.ProductSizeCreateDto;
import com.emenu.features.main.dto.response.ProductSizeDto;
import com.emenu.features.main.dto.update.ProductSizeUpdateDto;
import com.emenu.features.main.models.ProductSize;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductSizeMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(source = "promotionType", target = "promotionType", qualifiedByName = "sizeStringToPromotionType")
    @Mapping(target = "product", ignore = true)
    ProductSize toEntity(ProductSizeCreateDto dto);

    @Mapping(target = "productId", ignore = true)
    @Mapping(source = "promotionType", target = "promotionType", qualifiedByName = "sizeStringToPromotionType")
    @Mapping(target = "product", ignore = true)
    @AfterMapping
    default void afterSizeUpdate(ProductSizeUpdateDto dto, @MappingTarget ProductSize entity) {
        if (!dto.hasPromotionData()) {
            entity.removePromotion();
        }
    }

    void updateEntity(ProductSizeUpdateDto dto, @MappingTarget ProductSize entity);

    @Mapping(target = "productId", ignore = true)
    @Mapping(source = "promotionType", target = "promotionType", qualifiedByName = "sizeStringToPromotionType")
    @Mapping(target = "product", ignore = true)
    ProductSize toEntityFromUpdate(ProductSizeUpdateDto dto);

    @Mapping(target = "finalPrice", expression = "java(entity.getFinalPrice())")
    @Mapping(target = "hasPromotion", expression = "java(entity.isPromotionActive())")
    @Mapping(source = "promotionType", target = "promotionType", qualifiedByName = "sizePromotionTypeToString")
    ProductSizeDto toDto(ProductSize entity);

    List<ProductSizeDto> toDtos(List<ProductSize> entities);

    default List<ProductSize> toEntitiesFromUpdate(List<ProductSizeUpdateDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .filter(dto -> !dto.shouldDelete() && dto.isNew())
                .map(dto -> {
                    ProductSize size = toEntityFromUpdate(dto);
                    if (!dto.hasPromotionData()) {
                        size.removePromotion();
                    }
                    return size;
                })
                .toList();
    }

    default List<java.util.UUID> getIdsToDelete(List<ProductSizeUpdateDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .filter(dto -> dto.shouldDelete() && dto.isExisting())
                .map(ProductSizeUpdateDto::getId)
                .toList();
    }

    default List<ProductSizeUpdateDto> getExistingToUpdate(List<ProductSizeUpdateDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .filter(dto -> !dto.shouldDelete() && dto.isExisting())
                .toList();
    }

    @Named("sizeStringToPromotionType")
    default PromotionType sizeStringToPromotionType(String promotionType) {
        if (promotionType == null || promotionType.trim().isEmpty()) {
            return null;
        }
        try {
            return PromotionType.valueOf(promotionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("sizePromotionTypeToString")
    default String sizePromotionTypeToString(PromotionType promotionType) {
        return promotionType != null ? promotionType.name() : null;
    }
}