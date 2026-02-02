package com.emenu.features.main.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.auth.models.User;
import com.emenu.features.main.dto.filter.ProductFilterDto;
import com.emenu.features.main.dto.response.FavoriteRemoveAllDto;
import com.emenu.features.main.dto.response.FavoriteToggleDto;
import com.emenu.features.main.dto.response.ProductListDto;
import com.emenu.features.main.mapper.FavoriteMapper;
import com.emenu.features.main.mapper.ProductMapper;
import com.emenu.features.main.models.Product;
import com.emenu.features.main.models.ProductFavorite;
import com.emenu.features.main.repository.ProductFavoriteRepository;
import com.emenu.features.main.repository.ProductRepository;
import com.emenu.features.main.service.ProductFavoriteService;
import com.emenu.security.SecurityUtils;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import com.emenu.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductFavoriteServiceImpl implements ProductFavoriteService {

    private final ProductRepository productRepository;
    private final ProductFavoriteRepository favoriteRepository;
    private final ProductMapper productMapper;
    private final FavoriteMapper favoriteMapper;
    private final PaginationMapper paginationMapper;
    private final SecurityUtils securityUtils;

    /**
     * Toggle product favorite status for current user
     */
    @Override
    public FavoriteToggleDto toggleFavorite(UUID productId) {
        User currentUser = securityUtils.getCurrentUser();
        UUID userId = currentUser.getId();

        log.info("Toggling favorite - Product: {}, User: {}", productId, userId);

        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (!product.isActive()) {
            throw new ValidationException("Cannot favorite inactive product");
        }

        boolean isFavorited = favoriteRepository.existsByUserIdAndProductIdAndIsDeletedFalse(userId, productId);

        String action;
        boolean finalStatus;

        if (!isFavorited) {
            ProductFavorite favorite = new ProductFavorite(userId, productId);
            favoriteRepository.save(favorite);
            productRepository.incrementFavoriteCount(productId);
            action = "added";
            finalStatus = true;
            log.info("Favorite added - Product: {}, User: {}", productId, userId);
        } else {
            favoriteRepository.deleteByUserIdAndProductId(userId, productId);
            productRepository.decrementFavoriteCount(productId);
            action = "removed";
            finalStatus = false;
            log.info("Favorite removed - Product: {}, User: {}", productId, userId);
        }

        return favoriteMapper.createToggleResponse(productId, userId, finalStatus, action);
    }

    /**
     * Get paginated list of user's favorite products
     */
    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProductListDto> getUserFavorites(ProductFilterDto filter) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("Getting favorites - User: {}", userId);

        Pageable pageable = PaginationUtils.createPageable(
            filter.getPageNo(),
            filter.getPageSize(),
            "createdAt",
            "DESC"
        );

        Page<Product> favoritePage = productRepository.findUserFavorites(userId, pageable);

        PaginationResponse<ProductListDto> response = productMapper.toPaginationResponse(
            favoritePage,
            paginationMapper
        );

        response.getContent().forEach(product -> product.setIsFavorited(true));

        log.info("Retrieved {} favorites - User: {}", response.getContent().size(), userId);
        return response;
    }

    /**
     * Remove all favorites for current user
     */
    @Override
    public FavoriteRemoveAllDto removeAllFavorites() {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("Removing all favorites - User: {}", userId);

        int removedCount = favoriteRepository.deleteAllByUserId(userId);

        log.info("Removed {} favorites - User: {}", removedCount, userId);

        return FavoriteRemoveAllDto.builder()
                .userId(userId)
                .removedCount(removedCount)
                .timestamp(LocalDateTime.now())
                .message(String.format("Removed %d products from favorites", removedCount))
                .build();
    }
}