package com.emenu.features.main.repository;

import com.emenu.enums.order.OrderStatus;
import com.emenu.features.main.models.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH o.paymentMethod pm " +
            "LEFT JOIN FETCH o.location l " +
            "WHERE o.id = :id AND o.isDeleted = false")
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH o.paymentMethod pm " +
            "LEFT JOIN FETCH o.location l " +
            "WHERE o.id = :id AND o.userId = :userId AND o.isDeleted = false")
    Optional<Order> findByIdAndUserIdWithDetails(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.paymentMethod pm " +
            "WHERE o.userId = :userId AND o.isDeleted = false " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.isDeleted = false " +
            "AND (:status IS NULL OR o.status = :status) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdWithFilter(
            @Param("userId") UUID userId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    // Admin filtering - comprehensive
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN o.user u " +
            "WHERE o.isDeleted = false " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:userId IS NULL OR o.userId = :userId) " +
            "AND (:paymentMethodId IS NULL OR o.paymentMethodId = :paymentMethodId) " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    Page<Order> findAllWithFilter(
            @Param("search") String search,
            @Param("status") OrderStatus status,
            @Param("userId") UUID userId,
            @Param("paymentMethodId") UUID paymentMethodId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.isDeleted = false")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.isDeleted = false")
    long countByStatus(@Param("status") OrderStatus status);

    boolean existsByOrderNumber(String orderNumber);
}
