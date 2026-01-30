package com.emenu.features.main.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.auth.models.User;
import com.emenu.features.main.dto.filter.CategoryAllFilterRequest;
import com.emenu.features.main.dto.filter.CategoryFilterRequest;
import com.emenu.features.main.dto.request.CategoryCreateRequest;
import com.emenu.features.main.dto.response.CategoryResponse;
import com.emenu.features.main.dto.update.CategoryUpdateRequest;
import com.emenu.features.main.mapper.CategoryMapper;
import com.emenu.features.main.models.Category;
import com.emenu.features.main.repository.CategoryRepository;
import com.emenu.features.main.service.CategoryService;
import com.emenu.security.SecurityUtils;
import com.emenu.shared.dto.PaginationResponse;
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
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SecurityUtils securityUtils;
    private final com.emenu.shared.mapper.PaginationMapper paginationMapper;

    @Override
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        log.info("Creating category: {}", request.getName());

        // Check if category name already exists for this business
        if (categoryRepository.existsByNameAndIsDeletedFalse(
                request.getName())) {
            throw new ValidationException("Category name already exists in your business");
        }

        Category category = categoryMapper.toEntity(request);

        Category savedCategory = categoryRepository.save(category);

        log.info("Category created successfully: {}", savedCategory.getName());
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<CategoryResponse> getAllCategories(CategoryFilterRequest filter) {
        Pageable pageable = PaginationUtils.createPageable(
                filter.getPageNo(), filter.getPageSize(), filter.getSortBy(), filter.getSortDirection()
        );

        Page<Category> categoryPage = categoryRepository.findAllWithFilters(
                filter.getStatus(),
                filter.getSearch(),
                pageable
        );
        return categoryMapper.toPaginationResponse(categoryPage, paginationMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllItemCategories(CategoryAllFilterRequest filter) {
        List<Category> categories = categoryRepository.findAllWithFilters(
                filter.getStatus(),
                filter.getSearch(),
                PaginationUtils.createSort(filter.getSortBy(), filter.getSortDirection())
        );
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = findCategoryById(id);
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(UUID id, CategoryUpdateRequest request) {
        Category category = findCategoryById(id);

        // Check if new name already exists (if name is being changed)
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByNameAndIsDeletedFalse(
                    request.getName())) {
                throw new ValidationException("Category name already exists");
            }
        }

        categoryMapper.updateEntity(request, category);
        Category updatedCategory = categoryRepository.save(category);

        log.info("Category updated successfully: {}", id);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public CategoryResponse deleteCategory(UUID id) {
        Category category = findCategoryById(id);

        category.softDelete();
        category = categoryRepository.save(category);

        log.info("Category deleted successfully: {}", id);
        return categoryMapper.toResponse(category);
    }

    // Private helper methods
    private Category findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}