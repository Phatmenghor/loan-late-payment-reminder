package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.MyOrderFilterRequest;
import com.emenu.features.main.dto.request.CheckoutRequest;
import com.emenu.features.main.dto.response.OrderResponse;
import com.emenu.features.main.service.OrderService;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Checkout - Create order from cart
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@Valid @RequestBody CheckoutRequest request) {
        log.info("Processing checkout");
        OrderResponse order = orderService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    /**
     * Get my orders with filter
     */
    @PostMapping("/my-orders")
    public ResponseEntity<ApiResponse<PaginationResponse<OrderResponse>>> getMyOrders(
            @Valid @RequestBody MyOrderFilterRequest filter) {
        log.info("Getting my orders");
        PaginationResponse<OrderResponse> orders = orderService.getMyOrders(filter);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * Get my order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrderById(@PathVariable UUID id) {
        log.info("Getting my order by ID: {}", id);
        OrderResponse order = orderService.getMyOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    /**
     * Cancel my order (only pending orders)
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID id) {
        log.info("Cancelling order: {}", id);
        OrderResponse order = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }
}
