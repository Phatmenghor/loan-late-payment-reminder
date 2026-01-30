package com.emenu.features.main.mapper;

import com.emenu.features.main.dto.request.SubCategoryCreateRequest;
import com.emenu.features.main.dto.response.SubCategoryResponse;
import com.emenu.features.main.dto.update.SubCategoryUpdateRequest;
import com.emenu.features.main.models.SubCategory;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaginationMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubCategoryMapper {

    @Mapping(target = "category", ignore = true)
    SubCategory toEntity(SubCategoryCreateRequest request);

    @Mapping(source = "category.name", target = "categoryName")
    SubCategoryResponse toResponse(SubCategory subCategory);

    List<SubCategoryResponse> toResponseList(List<SubCategory> subCategories);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", ignore = true)
    void updateEntity(SubCategoryUpdateRequest request, @MappingTarget SubCategory subCategory);

    default PaginationResponse<SubCategoryResponse> toPaginationResponse(Page<SubCategory> page, PaginationMapper paginationMapper) {
        return paginationMapper.toPaginationResponse(page, this::toResponseList);
    }
}
