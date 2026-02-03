package com.emenu.features.main.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.features.auth.models.User;
import com.emenu.features.main.dto.request.CartItemCreateRequest;
import com.emenu.features.main.dto.response.CartItemResponse;
import com.emenu.features.main.dto.response.CartSummaryResponse;
import com.emenu.features.main.mapper.CartItemMapper;
import com.emenu.features.main.models.CartItem;
import com.emenu.features.main.models.Product;
import com.emenu.features.main.models.ProductSize;
import com.emenu.features.main.repository.CartItemRepository;
import com.emenu.features.main.repository.ProductRepository;
import com.emenu.features.main.repository.ProductSizeRepository;
import com.emenu.features.main.service.CartService;
import com.emenu.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final CartItemMapper cartItemMapper;
    private final SecurityUtils securityUtils;

    @Override
    public CartSummaryResponse submitCartItem(CartItemCreateRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductIdAndProductSizeId(
                currentUser.getId(), request.getProductId(), request.getProductSizeId()
        );

        if (request.getQuantity() == 0) {
            // qty 0 = remove item
            existing.ifPresent(cartItem -> {
                cartItem.softDelete();
                cartItemRepository.save(cartItem);
                log.info("Cart item removed for user: {}, product: {}", currentUser.getId(), request.getProductId());
            });
        } else {
            // qty >= 1 = add or update
            if (existing.isPresent()) {
                CartItem cartItem = existing.get();
                cartItem.setQuantity(request.getQuantity());
                if (request.getNote() != null) {
                    cartItem.setNote(request.getNote());
                }
                cartItemRepository.save(cartItem);
                log.info("Cart item updated for user: {}, product: {}, qty: {}", currentUser.getId(), request.getProductId(), request.getQuantity());
            } else {
                Product product = productRepository.findByIdAndIsDeletedFalse(request.getProductId())
                        .orElseThrow(() -> new NotFoundException("Product not found"));

                BigDecimal originalPrice;
                if (request.getProductSizeId() != null) {
                    ProductSize size = productSizeRepository.findById(request.getProductSizeId())
                            .orElseThrow(() -> new NotFoundException("Product size not found"));
                    originalPrice = size.getPrice();
                } else {
                    originalPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                }

                CartItem cartItem = new CartItem();
                cartItem.setUserId(currentUser.getId());
                cartItem.setProductId(request.getProductId());
                cartItem.setProductSizeId(request.getProductSizeId());
                cartItem.setQuantity(request.getQuantity());
                cartItem.setOriginalPrice(originalPrice);
                cartItem.setNote(request.getNote());
                cartItemRepository.save(cartItem);
                log.info("Cart item created for user: {}, product: {}, qty: {}", currentUser.getId(), request.getProductId(), request.getQuantity());
            }
        }

        return buildCartSummary(currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CartSummaryResponse getCart() {
        User currentUser = securityUtils.getCurrentUser();
        return buildCartSummary(currentUser.getId());
    }

    @Override
    public CartSummaryResponse clearCart() {
        User currentUser = securityUtils.getCurrentUser();
        cartItemRepository.clearCartByUserId(currentUser.getId());
        log.info("Cart cleared for user: {}", currentUser.getId());
        return buildCartSummary(currentUser.getId());
    }

    private CartSummaryResponse buildCartSummary(UUID userId) {
        List<CartItem> items = cartItemRepository.findByUserIdWithDetails(userId);
        List<CartItemResponse> responseItems = items.stream()
                .map(this::buildCartItemResponse)
                .toList();

        int totalItems = responseItems.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalOriginalPrice = responseItems.stream()
                .map(CartItemResponse::getTotalOriginalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayment = responseItems.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = totalOriginalPrice.subtract(totalPayment);

        return CartSummaryResponse.builder()
                .items(responseItems)
                .totalItems(totalItems)
                .totalOriginalPrice(totalOriginalPrice)
                .totalDiscount(totalDiscount)
                .totalPayment(totalPayment)
                .build();
    }

    private CartItemResponse buildCartItemResponse(CartItem cartItem) {
        CartItemResponse response = new CartItemResponse();
        response.setId(cartItem.getId());
        response.setCreatedAt(cartItem.getCreatedAt());
        response.setUpdatedAt(cartItem.getUpdatedAt());
        response.setCreatedBy(cartItem.getCreatedBy());
        response.setUpdatedBy(cartItem.getUpdatedBy());
        response.setProductId(cartItem.getProductId());
        response.setProductSizeId(cartItem.getProductSizeId());
        response.setQuantity(cartItem.getQuantity());
        response.setNote(cartItem.getNote());

        Product product = cartItem.getProduct();
        ProductSize productSize = cartItem.getProductSize();

        if (product != null) {
            response.setProductName(product.getName());
            response.setProductMainImageUrl(product.getMainImageUrl());
        }

        if (productSize != null) {
            response.setProductSizeName(productSize.getName());
            response.setOriginalPrice(productSize.getPrice());
            response.setDisplayPrice(productSize.getFinalPrice());
            response.setUnitPrice(productSize.getFinalPrice());
            response.setHasActivePromotion(productSize.isPromotionActive());
            if (productSize.getPromotionType() != null) {
                response.setPromotionType(productSize.getPromotionType().name());
            }
            response.setPromotionValue(productSize.getPromotionValue());
            response.setPromotionFromDate(productSize.getPromotionFromDate());
            response.setPromotionToDate(productSize.getPromotionToDate());
        } else if (product != null) {
            response.setOriginalPrice(product.getDisplayOriginPrice());
            response.setDisplayPrice(product.getDisplayPrice());
            response.setUnitPrice(product.getDisplayPrice());
            response.setHasActivePromotion(product.getHasActivePromotion());
            if (product.getDisplayPromotionType() != null) {
                response.setPromotionType(product.getDisplayPromotionType().name());
            }
            response.setPromotionValue(product.getDisplayPromotionValue());
            response.setPromotionFromDate(product.getDisplayPromotionFromDate());
            response.setPromotionToDate(product.getDisplayPromotionToDate());
        } else {
            response.setOriginalPrice(cartItem.getOriginalPrice());
            response.setDisplayPrice(cartItem.getOriginalPrice());
            response.setUnitPrice(cartItem.getOriginalPrice());
            response.setHasActivePromotion(false);
        }

        BigDecimal unitPrice = response.getUnitPrice() != null ? response.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal originalPrice = response.getOriginalPrice() != null ? response.getOriginalPrice() : BigDecimal.ZERO;
        int quantity = response.getQuantity() != null ? response.getQuantity() : 0;

        response.setTotalOriginalPrice(originalPrice.multiply(BigDecimal.valueOf(quantity)));
        response.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        response.setDiscountAmount(response.getTotalOriginalPrice().subtract(response.getTotalPrice()));

        return response;
    }
}
