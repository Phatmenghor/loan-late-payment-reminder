package com.emenu.features.main.service;

import com.emenu.features.main.dto.filter.SubCategoryAllFilterRequest;
import com.emenu.features.main.dto.filter.SubCategoryFilterRequest;
import com.emenu.features.main.dto.request.SubCategoryCreateRequest;
import com.emenu.features.main.dto.response.SubCategoryResponse;
import com.emenu.features.main.dto.update.SubCategoryUpdateRequest;
import com.emenu.shared.dto.PaginationResponse;

import java.util.List;
import java.util.UUID;

public interface SubCategoryService {

    SubCategoryResponse createSubCategory(SubCategoryCreateRequest request);
    PaginationResponse<SubCategoryResponse> getAllSubCategories(SubCategoryFilterRequest filter);
    List<SubCategoryResponse> getAllItemSubCategories(SubCategoryAllFilterRequest filter);
    SubCategoryResponse getSubCategoryById(UUID id);
    SubCategoryResponse updateSubCategory(UUID id, SubCategoryUpdateRequest request);
    SubCategoryResponse deleteSubCategory(UUID id);
}
