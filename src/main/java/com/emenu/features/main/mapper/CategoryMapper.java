package com.emenu.features.main.mapper;

import com.emenu.features.main.dto.request.CategoryCreateRequest;
import com.emenu.features.main.dto.response.CategoryResponse;
import com.emenu.features.main.dto.update.CategoryUpdateRequest;
import com.emenu.features.main.models.Category;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaginationMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    Category toEntity(CategoryCreateRequest request);

    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(CategoryUpdateRequest request, @MappingTarget Category category);

    default PaginationResponse<CategoryResponse> toPaginationResponse(Page<Category> categoryPage, PaginationMapper paginationMapper) {
        return paginationMapper.toPaginationResponse(categoryPage, this::toResponseList);
    }
}
