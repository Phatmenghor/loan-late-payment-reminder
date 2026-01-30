package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.SubCategoryFilterRequest;
import com.emenu.features.main.dto.request.SubCategoryCreateRequest;
import com.emenu.features.main.dto.response.SubCategoryResponse;
import com.emenu.features.main.dto.update.SubCategoryUpdateRequest;
import com.emenu.features.main.service.SubCategoryService;
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
@RequestMapping("/api/v1/sub-categories")
@RequiredArgsConstructor
@Slf4j
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubCategoryResponse>> createSubCategory(@Valid @RequestBody SubCategoryCreateRequest request) {
        log.info("Creating sub-category: {}", request.getName());
        SubCategoryResponse subCategory = subCategoryService.createSubCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("SubCategory created successfully", subCategory));
    }

    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<SubCategoryResponse>>> getAllSubCategories(@Valid @RequestBody SubCategoryFilterRequest filter) {
        log.info("Getting all sub-categories");
        PaginationResponse<SubCategoryResponse> subCategories = subCategoryService.getAllSubCategories(filter);
        return ResponseEntity.ok(ApiResponse.success("SubCategories retrieved successfully", subCategories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> getSubCategoryById(@PathVariable UUID id) {
        log.info("Getting sub-category by ID: {}", id);
        SubCategoryResponse subCategory = subCategoryService.getSubCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("SubCategory retrieved successfully", subCategory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> updateSubCategory(
            @PathVariable UUID id,
            @Valid @RequestBody SubCategoryUpdateRequest request) {
        log.info("Updating sub-category: {}", id);
        SubCategoryResponse subCategory = subCategoryService.updateSubCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("SubCategory updated successfully", subCategory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> deleteSubCategory(@PathVariable UUID id) {
        log.info("Deleting sub-category: {}", id);
        SubCategoryResponse subCategory = subCategoryService.deleteSubCategory(id);
        return ResponseEntity.ok(ApiResponse.success("SubCategory deleted successfully", subCategory));
    }
}
