package com.emenu.features.main.repository;

import com.emenu.features.main.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Query("SELECT ci FROM CartItem ci " +
            "LEFT JOIN FETCH ci.product p " +
            "LEFT JOIN FETCH ci.productSize ps " +
            "WHERE ci.userId = :userId AND ci.isDeleted = false " +
            "AND p.isDeleted = false " +
            "ORDER BY ci.createdAt DESC")
    List<CartItem> findByUserIdWithDetails(@Param("userId") UUID userId);

    @Query("SELECT ci FROM CartItem ci " +
            "WHERE ci.userId = :userId AND ci.productId = :productId " +
            "AND (:productSizeId IS NULL AND ci.productSizeId IS NULL OR ci.productSizeId = :productSizeId) " +
            "AND ci.isDeleted = false")
    Optional<CartItem> findByUserIdAndProductIdAndProductSizeId(
            @Param("userId") UUID userId,
            @Param("productId") UUID productId,
            @Param("productSizeId") UUID productSizeId
    );

    @Query("SELECT ci FROM CartItem ci " +
            "WHERE ci.id = :id AND ci.userId = :userId AND ci.isDeleted = false")
    Optional<CartItem> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE CartItem ci SET ci.isDeleted = true WHERE ci.userId = :userId AND ci.isDeleted = false")
    void clearCartByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.userId = :userId AND ci.isDeleted = false")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT ci.productId FROM CartItem ci WHERE ci.userId = :userId AND ci.isDeleted = false AND ci.productId IN :productIds")
    List<UUID> findProductIdsInCart(@Param("userId") UUID userId, @Param("productIds") List<UUID> productIds);
}
