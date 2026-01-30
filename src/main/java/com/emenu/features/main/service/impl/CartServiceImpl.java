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
        List<CartItemResponse> responseItems = cartItemMapper.toResponseList(items);

        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal totalPrice = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartSummaryResponse(responseItems, totalItems, totalPrice);
    }
}
