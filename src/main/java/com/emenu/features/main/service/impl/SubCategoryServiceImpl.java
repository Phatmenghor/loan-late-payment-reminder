package com.emenu.features.main.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.main.dto.filter.SubCategoryAllFilterRequest;
import com.emenu.features.main.dto.filter.SubCategoryFilterRequest;
import com.emenu.features.main.dto.request.SubCategoryCreateRequest;
import com.emenu.features.main.dto.response.SubCategoryResponse;
import com.emenu.features.main.dto.update.SubCategoryUpdateRequest;
import com.emenu.features.main.mapper.SubCategoryMapper;
import com.emenu.features.main.models.SubCategory;
import com.emenu.features.main.repository.CategoryRepository;
import com.emenu.features.main.repository.SubCategoryRepository;
import com.emenu.features.main.service.SubCategoryService;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import com.emenu.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryMapper subCategoryMapper;
    private final PaginationMapper paginationMapper;

    @Override
    public SubCategoryResponse createSubCategory(SubCategoryCreateRequest request) {
        log.info("Creating sub-category: {}", request.getName());

        categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (subCategoryRepository.existsByNameAndCategoryIdAndIsDeletedFalse(
                request.getName(), request.getCategoryId())) {
            throw new ValidationException("SubCategory name already exists in this category");
        }

        SubCategory subCategory = subCategoryMapper.toEntity(request);
        SubCategory saved = subCategoryRepository.save(subCategory);

        log.info("SubCategory created successfully: {}", saved.getName());
        return subCategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<SubCategoryResponse> getAllSubCategories(SubCategoryFilterRequest filter) {
        Pageable pageable = PaginationUtils.createPageable(
                filter.getPageNo(), filter.getPageSize(), filter.getSortBy(), filter.getSortDirection()
        );

        Page<SubCategory> page = subCategoryRepository.findAllWithFilters(
                filter.getStatus(),
                filter.getCategoryId(),
                filter.getSearch(),
                pageable
        );
        return subCategoryMapper.toPaginationResponse(page, paginationMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getAllItemSubCategories(SubCategoryAllFilterRequest filter) {
        List<SubCategory> subCategories = subCategoryRepository.findAllWithFilters(
                filter.getStatus(),
                filter.getCategoryId(),
                filter.getSearch(),
                PaginationUtils.createSort(filter.getSortBy(), filter.getSortDirection())
        );
        return subCategoryMapper.toResponseList(subCategories);
    }

    @Override
    @Transactional(readOnly = true)
    public SubCategoryResponse getSubCategoryById(UUID id) {
        SubCategory subCategory = findSubCategoryById(id);
        return subCategoryMapper.toResponse(subCategory);
    }

    @Override
    public SubCategoryResponse updateSubCategory(UUID id, SubCategoryUpdateRequest request) {
        SubCategory subCategory = findSubCategoryById(id);

        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }

        if (request.getName() != null && !request.getName().equals(subCategory.getName())) {
            UUID catId = request.getCategoryId() != null ? request.getCategoryId() : subCategory.getCategoryId();
            if (subCategoryRepository.existsByNameAndCategoryIdAndIsDeletedFalse(request.getName(), catId)) {
                throw new ValidationException("SubCategory name already exists in this category");
            }
        }

        subCategoryMapper.updateEntity(request, subCategory);
        SubCategory updated = subCategoryRepository.save(subCategory);

        log.info("SubCategory updated successfully: {}", id);
        return subCategoryMapper.toResponse(updated);
    }

    @Override
    public SubCategoryResponse deleteSubCategory(UUID id) {
        SubCategory subCategory = findSubCategoryById(id);
        subCategory.softDelete();
        subCategory = subCategoryRepository.save(subCategory);

        log.info("SubCategory deleted successfully: {}", id);
        return subCategoryMapper.toResponse(subCategory);
    }

    private SubCategory findSubCategoryById(UUID id) {
        return subCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SubCategory not found"));
    }
}
