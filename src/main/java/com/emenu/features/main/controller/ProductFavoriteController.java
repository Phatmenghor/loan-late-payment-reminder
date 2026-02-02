package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.ProductFilterDto;
import com.emenu.features.main.dto.response.FavoriteRemoveAllDto;
import com.emenu.features.main.dto.response.FavoriteToggleDto;
import com.emenu.features.main.dto.response.ProductListDto;
import com.emenu.features.main.service.ProductFavoriteService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/product-favorites")
@RequiredArgsConstructor
@Slf4j
public class ProductFavoriteController {

    private final ProductFavoriteService favoriteService;

    /**
     * Toggle favorite status for a product
     */
    @PostMapping("/{productId}/toggle")
    public ResponseEntity<ApiResponse<FavoriteToggleDto>> toggleFavorite(@PathVariable UUID productId) {
        log.info("Toggle favorite - Product: {}", productId);

        FavoriteToggleDto result = favoriteService.toggleFavorite(productId);

        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }

    /**
     * Get paginated list of user's favorite products
     */
    @PostMapping("/my-favorites")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductListDto>>> getUserFavorites(
            @Valid @RequestBody ProductFilterDto filter) {
        log.info("Get user favorites");
        PaginationResponse<ProductListDto> favorites = favoriteService.getUserFavorites(filter);
        return ResponseEntity.ok(ApiResponse.success("Favorite products retrieved successfully", favorites));
    }

    /**
     * Remove all favorites for current user
     */
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<FavoriteRemoveAllDto>> removeAllFavorites() {
        log.info("Remove all favorites");
        FavoriteRemoveAllDto result = favoriteService.removeAllFavorites();
        return ResponseEntity.ok(ApiResponse.success("All favorites removed successfully", result));
    }
}