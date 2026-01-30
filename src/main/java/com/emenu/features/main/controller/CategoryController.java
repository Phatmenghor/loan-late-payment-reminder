package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.CategoryFilterRequest;
import com.emenu.features.main.dto.request.CategoryCreateRequest;
import com.emenu.features.main.dto.response.CategoryResponse;
import com.emenu.features.main.dto.update.CategoryUpdateRequest;
import com.emenu.features.main.service.CategoryService;
import com.emenu.security.SecurityUtils;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;
    private final SecurityUtils securityUtils;

    /**
     * Create new category
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        log.info("Creating category: {}", request.getName());
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", category));
    }

    /**
     * Get all categories w
     */
    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<CategoryResponse>>> getAllCategories(@Valid @RequestBody CategoryFilterRequest filter) {
        log.info("Getting all categories");
        PaginationResponse<CategoryResponse> categories = categoryService.getAllCategories(filter);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    /**
     * Get category by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        log.info("Getting category by ID: {}", id);
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", category));
    }

    /**
     * Update category
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        log.info("Updating category: {}", id);
        CategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", category));
    }

    /**
     * Delete category
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> deleteCategory(@PathVariable UUID id) {
        log.info("Deleting category: {}", id);
        CategoryResponse category = categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", category));
    }
}