package com.emenu.features.main.repository;

import com.emenu.enums.product.ProductStatus;
import com.emenu.features.main.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.category c " +
            "LEFT JOIN FETCH p.sizes sz " +
            "WHERE p.id = :id AND p.isDeleted = false " +
            "AND (sz.isDeleted = false OR sz.isDeleted IS NULL)")
    Optional<Product> findByIdWithAllDetails(@Param("id") UUID id);

    Optional<Product> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT COUNT(p) FROM Product p " +
            "WHERE p.categoryId = :categoryId AND p.isDeleted = false")
    long countByCategoryId(@Param("categoryId") UUID categoryId);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :productId")
    int incrementViewCount(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE Product p SET p.favoriteCount = COALESCE(p.favoriteCount, 0) + 1 WHERE p.id = :productId")
    void incrementFavoriteCount(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE Product p SET p.favoriteCount = GREATEST(0, COALESCE(p.favoriteCount, 0) - 1) WHERE p.id = :productId")
    void decrementFavoriteCount(@Param("productId") UUID productId);

    @Query("SELECT p FROM Product p " +
            "INNER JOIN ProductFavorite pf ON p.id = pf.productId " +
            "WHERE pf.userId = :userId AND p.isDeleted = false AND pf.isDeleted = false")
    Page<Product> findUserFavorites(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.category c " +
            "WHERE p.isDeleted = false " +
            "AND (:categoryId IS NULL OR p.categoryId = :categoryId) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:hasPromotion IS NULL OR p.hasActivePromotion = :hasPromotion) " +
            "AND (:minPrice IS NULL OR p.displayPrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.displayPrice <= :maxPrice) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findAllWithFilters(
            @Param("categoryId") UUID categoryId,
            @Param("status") ProductStatus status,
            @Param("hasPromotion") Boolean hasPromotion,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.category c " +
            "WHERE p.isDeleted = false " +
            "AND (:categoryId IS NULL OR p.categoryId = :categoryId) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:hasPromotion IS NULL OR p.hasActivePromotion = :hasPromotion) " +
            "AND (:minPrice IS NULL OR p.displayPrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.displayPrice <= :maxPrice) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> findAllWithFilters(
            @Param("categoryId") UUID categoryId,
            @Param("status") ProductStatus status,
            @Param("hasPromotion") Boolean hasPromotion,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            Sort sort
    );
}
