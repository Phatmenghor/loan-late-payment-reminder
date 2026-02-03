package com.emenu.features.main.service;

import com.emenu.features.main.dto.filter.PaymentMethodFilterRequest;
import com.emenu.features.main.dto.request.PaymentMethodCreateRequest;
import com.emenu.features.main.dto.response.PaymentMethodResponse;
import com.emenu.features.main.dto.update.PaymentMethodUpdateRequest;
import com.emenu.shared.dto.PaginationResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentMethodService {

    // Admin operations
    PaymentMethodResponse createPaymentMethod(PaymentMethodCreateRequest request);
    PaginationResponse<PaymentMethodResponse> getAllPaymentMethods(PaymentMethodFilterRequest filter);
    PaymentMethodResponse getPaymentMethodById(UUID id);
    PaymentMethodResponse updatePaymentMethod(UUID id, PaymentMethodUpdateRequest request);
    PaymentMethodResponse deletePaymentMethod(UUID id);

    // Public operations
    List<PaymentMethodResponse> getActivePaymentMethods();
}
