package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.PaymentMethodFilterRequest;
import com.emenu.features.main.dto.request.PaymentMethodCreateRequest;
import com.emenu.features.main.dto.response.PaymentMethodResponse;
import com.emenu.features.main.dto.update.PaymentMethodUpdateRequest;
import com.emenu.features.main.service.PaymentMethodService;
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
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    /**
     * Create new payment method (Admin)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> createPaymentMethod(
            @Valid @RequestBody PaymentMethodCreateRequest request) {
        log.info("Creating payment method: {}", request.getName());
        PaymentMethodResponse paymentMethod = paymentMethodService.createPaymentMethod(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment method created successfully", paymentMethod));
    }

    /**
     * Get all payment methods with filters (Admin)
     */
    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<PaymentMethodResponse>>> getAllPaymentMethods(
            @Valid @RequestBody PaymentMethodFilterRequest filter) {
        log.info("Getting all payment methods");
        PaginationResponse<PaymentMethodResponse> paymentMethods = paymentMethodService.getAllPaymentMethods(filter);
        return ResponseEntity.ok(ApiResponse.success("Payment methods retrieved successfully", paymentMethods));
    }

    /**
     * Get payment method by ID (Admin)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> getPaymentMethodById(@PathVariable UUID id) {
        log.info("Getting payment method by ID: {}", id);
        PaymentMethodResponse paymentMethod = paymentMethodService.getPaymentMethodById(id);
        return ResponseEntity.ok(ApiResponse.success("Payment method retrieved successfully", paymentMethod));
    }

    /**
     * Update payment method (Admin)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> updatePaymentMethod(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentMethodUpdateRequest request) {
        log.info("Updating payment method: {}", id);
        PaymentMethodResponse paymentMethod = paymentMethodService.updatePaymentMethod(id, request);
        return ResponseEntity.ok(ApiResponse.success("Payment method updated successfully", paymentMethod));
    }

    /**
     * Delete payment method (Admin)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> deletePaymentMethod(@PathVariable UUID id) {
        log.info("Deleting payment method: {}", id);
        PaymentMethodResponse paymentMethod = paymentMethodService.deletePaymentMethod(id);
        return ResponseEntity.ok(ApiResponse.success("Payment method deleted successfully", paymentMethod));
    }
}
