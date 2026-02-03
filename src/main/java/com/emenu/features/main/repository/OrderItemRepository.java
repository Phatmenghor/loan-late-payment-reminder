package com.emenu.features.main.repository;

import com.emenu.features.main.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId AND oi.isDeleted = false")
    List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT oi FROM OrderItem oi " +
            "LEFT JOIN FETCH oi.product p " +
            "LEFT JOIN FETCH oi.productSize ps " +
            "WHERE oi.orderId = :orderId AND oi.isDeleted = false")
    List<OrderItem> findByOrderIdWithDetails(@Param("orderId") UUID orderId);
}
