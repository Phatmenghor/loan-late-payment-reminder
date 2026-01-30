package com.emenu.features.main.mapper;

import com.emenu.features.main.dto.request.ProductImageCreateDto;
import com.emenu.features.main.dto.response.ProductImageDto;
import com.emenu.features.main.dto.update.ProductImageUpdateDto;
import com.emenu.features.main.models.ProductImage;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductImageMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductImage toEntity(ProductImageCreateDto dto);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "product", ignore = true)
    void updateEntity(ProductImageUpdateDto dto, @MappingTarget ProductImage entity);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductImage toEntityFromUpdate(ProductImageUpdateDto dto);

    ProductImageDto toDto(ProductImage entity);

    List<ProductImageDto> toDtos(List<ProductImage> entities);

    default List<ProductImage> toEntitiesFromUpdate(List<ProductImageUpdateDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .filter(dto -> !dto.shouldDelete() && dto.isNew())
                .map(this::toEntityFromUpdate)
                .toList();
    }

    default List<java.util.UUID> getIdsToDelete(List<ProductImageUpdateDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .filter(dto -> dto.shouldDelete() && dto.isExisting())
                .map(ProductImageUpdateDto::getId)
                .toList();
    }

    default List<ProductImageUpdateDto> getExistingToUpdate(List<ProductImageUpdateDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .filter(dto -> !dto.shouldDelete() && dto.isExisting())
                .toList();
    }
}