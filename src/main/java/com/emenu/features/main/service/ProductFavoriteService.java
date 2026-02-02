package com.emenu.features.main.service;

import com.emenu.features.main.dto.filter.ProductFilterDto;
import com.emenu.features.main.dto.response.FavoriteRemoveAllDto;
import com.emenu.features.main.dto.response.FavoriteToggleDto;
import com.emenu.features.main.dto.response.ProductListDto;
import com.emenu.shared.dto.PaginationResponse;

import java.util.List;
import java.util.UUID;

public interface ProductFavoriteService {
    FavoriteToggleDto toggleFavorite(UUID productId);
    FavoriteToggleDto removeFavoriteById(UUID favoriteId);
    PaginationResponse<ProductListDto> getUserFavorites(ProductFilterDto filter);
    FavoriteRemoveAllDto removeAllFavorites();
    List<UUID> getFavoriteProductIds(UUID userId, List<UUID> productIds);
}