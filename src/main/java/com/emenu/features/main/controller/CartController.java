package com.emenu.features.main.controller;

import com.emenu.features.main.dto.request.CartItemCreateRequest;
import com.emenu.features.main.dto.response.CartSummaryResponse;
import com.emenu.features.main.service.CartService;
import com.emenu.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<ApiResponse<CartSummaryResponse>> submitCartItem(@Valid @RequestBody CartItemCreateRequest request) {
        log.info("Submit cart item - product: {}, qty: {}", request.getProductId(), request.getQuantity());
        CartSummaryResponse cart = cartService.submitCartItem(request);
        return ResponseEntity.ok(ApiResponse.success("Cart updated successfully", cart));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartSummaryResponse>> getCart() {
        log.info("Getting cart");
        CartSummaryResponse cart = cartService.getCart();
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartSummaryResponse>> clearCart() {
        log.info("Clearing cart");
        CartSummaryResponse cart = cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cart));
    }
}
