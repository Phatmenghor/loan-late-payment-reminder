package com.emenu.features.main.service.impl;

import com.emenu.enums.order.OrderStatus;
import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.models.UserLocation;
import com.emenu.features.auth.repository.UserLocationRepository;
import com.emenu.features.main.dto.filter.MyOrderFilterRequest;
import com.emenu.features.main.dto.filter.OrderFilterRequest;
import com.emenu.features.main.dto.request.CheckoutRequest;
import com.emenu.features.main.dto.response.OrderResponse;
import com.emenu.features.main.dto.update.OrderStatusUpdateRequest;
import com.emenu.features.main.mapper.OrderMapper;
import com.emenu.features.main.models.*;
import com.emenu.features.main.repository.*;
import com.emenu.features.main.service.OrderService;
import com.emenu.security.SecurityUtils;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.generate.OrderNumberGenerator;
import com.emenu.shared.mapper.PaginationMapper;
import com.emenu.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserLocationRepository userLocationRepository;
    private final OrderMapper orderMapper;
    private final PaginationMapper paginationMapper;
    private final SecurityUtils securityUtils;
    private final OrderNumberGenerator orderNumberGenerator;

    @Override
    public OrderResponse checkout(CheckoutRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        UUID userId = currentUser.getId();

        // Validate payment method
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdActiveNotDeleted(request.getPaymentMethodId())
                .orElseThrow(() -> new NotFoundException("Payment method not found or inactive"));

        // Validate location belongs to user
        UserLocation location = userLocationRepository.findByIdAndUserId(request.getLocationId(), userId)
                .orElseThrow(() -> new NotFoundException("Location not found"));

        // Get cart items
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithDetails(userId);
        if (cartItems.isEmpty()) {
            throw new ValidationException("Cart is empty");
        }

        // Generate unique order number
        String orderNumber = orderNumberGenerator.generateUniqueOrderNumber(orderRepository::existsByOrderNumber);

        // Calculate totals
        BigDecimal totalOriginalPrice = BigDecimal.ZERO;
        BigDecimal totalPayment = BigDecimal.ZERO;
        int totalItems = 0;

        // Create order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .locationId(request.getLocationId())
                .paymentMethodId(request.getPaymentMethodId())
                .status(OrderStatus.PENDING)
                .note(request.getNote())
                .build();

        // Create order items from cart
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            ProductSize productSize = cartItem.getProductSize();

            BigDecimal unitPrice;
            BigDecimal originalPrice;
            String sizeName = null;
            String sizeNameKh = null;

            if (productSize != null) {
                originalPrice = productSize.getPrice();
                unitPrice = productSize.getFinalPrice();
                sizeName = productSize.getName();
            } else if (product != null) {
                originalPrice = product.getDisplayOriginPrice() != null ? product.getDisplayOriginPrice() : product.getPrice();
                unitPrice = product.getDisplayPrice() != null ? product.getDisplayPrice() : product.getFinalPrice();
            } else {
                originalPrice = cartItem.getOriginalPrice();
                unitPrice = cartItem.getOriginalPrice();
            }

            BigDecimal discountAmount = originalPrice.subtract(unitPrice).multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productSizeId(cartItem.getProductSizeId())
                    .productName(product != null ? product.getName() : "Unknown Product")
                    .sizeName(sizeName)
                    .sizeNameKh(sizeNameKh)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(formatPrice(unitPrice))
                    .originalPrice(formatPrice(originalPrice))
                    .discountAmount(formatPrice(discountAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : discountAmount))
                    .totalPrice(formatPrice(totalPrice))
                    .note(cartItem.getNote())
                    .productImageUrl(product != null ? product.getMainImageUrl() : null)
                    .build();

            order.addOrderItem(orderItem);

            totalOriginalPrice = totalOriginalPrice.add(originalPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            totalPayment = totalPayment.add(totalPrice);
            totalItems += cartItem.getQuantity();
        }

        order.setTotalItems(totalItems);
        order.setTotalOriginalPrice(formatPrice(totalOriginalPrice));
        order.setTotalDiscount(formatPrice(totalOriginalPrice.subtract(totalPayment)));
        order.setTotalPayment(formatPrice(totalPayment));

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful checkout
        cartItemRepository.clearCartByUserId(userId);

        log.info("Order created successfully: {} for user: {}", savedOrder.getOrderNumber(), userId);

        // Fetch with details for response
        Order orderWithDetails = orderRepository.findByIdWithDetails(savedOrder.getId())
                .orElse(savedOrder);

        return buildOrderResponse(orderWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<OrderResponse> getMyOrders(MyOrderFilterRequest filter) {
        User currentUser = securityUtils.getCurrentUser();
        Pageable pageable = PaginationUtils.createPageable(
                filter.getPageNo(), filter.getPageSize(), filter.getSortBy(), filter.getSortDirection()
        );

        Page<Order> orderPage = orderRepository.findByUserIdWithFilter(
                currentUser.getId(),
                filter.getStatus(),
                pageable
        );

        return orderMapper.toPaginationResponse(orderPage, paginationMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(UUID orderId) {
        User currentUser = securityUtils.getCurrentUser();
        Order order = orderRepository.findByIdAndUserIdWithDetails(orderId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return buildOrderResponse(order);
    }

    @Override
    public OrderResponse cancelOrder(UUID orderId) {
        User currentUser = securityUtils.getCurrentUser();
        Order order = orderRepository.findByIdAndUserIdWithDetails(orderId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.canCancel()) {
            throw new ValidationException("Order cannot be cancelled. Only pending orders can be cancelled.");
        }

        order.cancel(currentUser.getUserIdentifier());
        Order savedOrder = orderRepository.save(order);

        log.info("Order cancelled: {} by user: {}", savedOrder.getOrderNumber(), currentUser.getId());
        return buildOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<OrderResponse> getAllOrders(OrderFilterRequest filter) {
        Pageable pageable = PaginationUtils.createPageable(
                filter.getPageNo(), filter.getPageSize(), filter.getSortBy(), filter.getSortDirection()
        );

        Page<Order> orderPage = orderRepository.findAllWithFilter(
                filter.getSearch(),
                filter.getStatus(),
                filter.getUserId(),
                filter.getPaymentMethodId(),
                filter.getFromDate(),
                filter.getToDate(),
                pageable
        );

        return orderMapper.toPaginationResponse(orderPage, paginationMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return buildOrderResponse(order);
    }

    @Override
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatusUpdateRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        OrderStatus newStatus = request.getStatus();

        // Validate status transition
        if (newStatus == OrderStatus.REJECTED) {
            if (!order.canUpdateByAdmin()) {
                throw new ValidationException("Cannot reject this order");
            }
            order.reject(currentUser.getUserIdentifier(), request.getRejectionReason());
        } else if (newStatus == OrderStatus.SUCCESS) {
            order.complete();
        } else if (newStatus == OrderStatus.CANCELLED) {
            throw new ValidationException("Admin cannot cancel orders. Only users can cancel their own orders.");
        } else {
            if (order.getStatus().isCompleted()) {
                throw new ValidationException("Cannot update status of completed/cancelled/rejected orders");
            }
            order.setStatus(newStatus);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {} by admin: {}", savedOrder.getOrderNumber(), newStatus, currentUser.getId());

        return buildOrderResponse(savedOrder);
    }

    private OrderResponse buildOrderResponse(Order order) {
        OrderResponse response = orderMapper.toResponse(order);
        if (order.getOrderItems() != null) {
            response.setOrderItems(orderMapper.toOrderItemResponseList(order.getOrderItems()));
        }
        return response;
    }

    private BigDecimal formatPrice(BigDecimal price) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
