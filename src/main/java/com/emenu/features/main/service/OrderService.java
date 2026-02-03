package com.emenu.features.main.service;

import com.emenu.features.main.dto.filter.MyOrderFilterRequest;
import com.emenu.features.main.dto.filter.OrderFilterRequest;
import com.emenu.features.main.dto.request.CheckoutRequest;
import com.emenu.features.main.dto.response.OrderResponse;
import com.emenu.features.main.dto.update.OrderStatusUpdateRequest;
import com.emenu.shared.dto.PaginationResponse;

import java.util.UUID;

public interface OrderService {

    // User operations
    OrderResponse checkout(CheckoutRequest request);
    PaginationResponse<OrderResponse> getMyOrders(MyOrderFilterRequest filter);
    OrderResponse getMyOrderById(UUID orderId);
    OrderResponse cancelOrder(UUID orderId);

    // Admin operations
    PaginationResponse<OrderResponse> getAllOrders(OrderFilterRequest filter);
    OrderResponse getOrderById(UUID orderId);
    OrderResponse updateOrderStatus(UUID orderId, OrderStatusUpdateRequest request);
}
