package com.emenu.features.main.service;

import com.emenu.features.main.dto.request.CartItemCreateRequest;
import com.emenu.features.main.dto.response.CartSummaryResponse;

public interface CartService {

    CartSummaryResponse submitCartItem(CartItemCreateRequest request);
    CartSummaryResponse getCart();
    CartSummaryResponse clearCart();
}
