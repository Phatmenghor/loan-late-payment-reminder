package com.emenu.features.main.controller;

import com.emenu.features.main.dto.response.PaymentMethodResponse;
import com.emenu.features.main.service.PaymentMethodService;
import com.emenu.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/payment-methods")
@RequiredArgsConstructor
@Slf4j
public class PublicPaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    /**
     * Get all active payment methods (for checkout)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> getActivePaymentMethods() {
        log.info("Getting active payment methods");
        List<PaymentMethodResponse> paymentMethods = paymentMethodService.getActivePaymentMethods();
        return ResponseEntity.ok(ApiResponse.success("Payment methods retrieved successfully", paymentMethods));
    }
}
