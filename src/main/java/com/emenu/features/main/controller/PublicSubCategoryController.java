package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.SubCategoryAllFilterRequest;
import com.emenu.features.main.dto.filter.SubCategoryFilterRequest;
import com.emenu.features.main.dto.response.SubCategoryResponse;
import com.emenu.features.main.service.SubCategoryService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/sub-categories")
@RequiredArgsConstructor
@Slf4j
public class PublicSubCategoryController {

    private final SubCategoryService subCategoryService;

    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<SubCategoryResponse>>> getAllSubCategories(@Valid @RequestBody SubCategoryFilterRequest filter) {
        log.info("Getting public sub-categories");
        PaginationResponse<SubCategoryResponse> subCategories = subCategoryService.getAllSubCategories(filter);
        return ResponseEntity.ok(ApiResponse.success("SubCategories retrieved successfully", subCategories));
    }

    @PostMapping("/all-data")
    public ResponseEntity<ApiResponse<List<SubCategoryResponse>>> getAllDataSubCategories(@Valid @RequestBody SubCategoryAllFilterRequest filter) {
        log.info("Getting all public sub-categories data");
        List<SubCategoryResponse> subCategories = subCategoryService.getAllItemSubCategories(filter);
        return ResponseEntity.ok(ApiResponse.success("SubCategories all retrieved successfully", subCategories));
    }
}
