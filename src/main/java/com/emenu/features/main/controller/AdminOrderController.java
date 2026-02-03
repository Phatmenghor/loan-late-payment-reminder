package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.OrderFilterRequest;
import com.emenu.features.main.dto.response.OrderResponse;
import com.emenu.features.main.dto.update.OrderStatusUpdateRequest;
import com.emenu.features.main.service.OrderService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Get all orders with comprehensive filters (Admin)
     * Supports filtering by:
     * - search (order number, customer name, phone)
     * - status (PENDING, PREPARING, ON_DELIVERY, SUCCESS, CANCELLED, REJECTED)
     * - userId
     * - paymentMethodId
     * - fromDate
     * - toDate
     */
    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<OrderResponse>>> getAllOrders(
            @Valid @RequestBody OrderFilterRequest filter) {
        log.info("Admin: Getting all orders with filter");
        PaginationResponse<OrderResponse> orders = orderService.getAllOrders(filter);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * Get order by ID with full details (Admin)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        log.info("Admin: Getting order by ID: {}", id);
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    /**
     * Update order status (Admin)
     * Available status updates:
     * - PREPARING: Start preparing the order
     * - ON_DELIVERY: Order is out for delivery
     * - SUCCESS: Mark order as completed
     * - REJECTED: Reject the order (requires rejectionReason)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info("Admin: Updating order {} status to {}", id, request.getStatus());
        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", order));
    }
}
