package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.BannerFilterRequest;
import com.emenu.features.main.dto.request.BannerCreateRequest;
import com.emenu.features.main.dto.response.BannerResponse;
import com.emenu.features.main.dto.update.BannerUpdateRequest;
import com.emenu.features.main.service.BannerService;
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
@RequestMapping("/api/v1/banner")
@RequiredArgsConstructor
@Slf4j
public class BannerController {

    private final BannerService bannerService;

    /**
     * Create new banner
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(@Valid @RequestBody BannerCreateRequest request) {
        log.info("Creating banner: {}", request.getImageUrl());
        BannerResponse bannerResponse = bannerService.createBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Banner created successfully", bannerResponse));
    }

    /**
     * Get all banner with filtering
     */
    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<BannerResponse>>> getAllBanner(@Valid @RequestBody BannerFilterRequest filter) {
        log.info("Getting all banner for current user's business");
        PaginationResponse<BannerResponse> bannerResponse = bannerService.getAllBanners(filter);
        return ResponseEntity.ok(ApiResponse.success("Banner retrieved successfully", bannerResponse));
    }

    /**
     * Get banner by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> getBannerById(@PathVariable UUID id) {
        log.info("Getting banner by ID: {}", id);
        BannerResponse bannerResponse = bannerService.getBannerById(id);
        return ResponseEntity.ok(ApiResponse.success("Banner retrieved successfully", bannerResponse));
    }

    /**
     * Update banner
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @PathVariable UUID id,
            @Valid @RequestBody BannerUpdateRequest request) {
        log.info("Updating banner: {}", id);
        BannerResponse bannerResponse = bannerService.updateBanner(id, request);
        return ResponseEntity.ok(ApiResponse.success("Banner updated successfully", bannerResponse));
    }

    /**
     * Delete banner
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> deleteBanner(@PathVariable UUID id) {
        log.info("Deleting category: {}", id);
        BannerResponse bannerResponse = bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success("Banner deleted successfully", bannerResponse));
    }
}