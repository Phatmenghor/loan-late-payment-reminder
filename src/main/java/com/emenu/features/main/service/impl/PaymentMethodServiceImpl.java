package com.emenu.features.main.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.features.main.dto.filter.PaymentMethodFilterRequest;
import com.emenu.features.main.dto.request.PaymentMethodCreateRequest;
import com.emenu.features.main.dto.response.PaymentMethodResponse;
import com.emenu.features.main.dto.update.PaymentMethodUpdateRequest;
import com.emenu.features.main.mapper.PaymentMethodMapper;
import com.emenu.features.main.models.PaymentMethod;
import com.emenu.features.main.repository.PaymentMethodRepository;
import com.emenu.features.main.service.PaymentMethodService;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import com.emenu.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;
    private final PaginationMapper paginationMapper;

    @Override
    public PaymentMethodResponse createPaymentMethod(PaymentMethodCreateRequest request) {
        log.info("Creating payment method: {}", request.getName());

        PaymentMethod paymentMethod = paymentMethodMapper.toEntity(request);
        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);

        log.info("Payment method created successfully: {}", savedPaymentMethod.getName());
        return paymentMethodMapper.toResponse(savedPaymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<PaymentMethodResponse> getAllPaymentMethods(PaymentMethodFilterRequest filter) {
        Pageable pageable = PaginationUtils.createPageable(
                filter.getPageNo(), filter.getPageSize(), filter.getSortBy(), filter.getSortDirection()
        );

        Page<PaymentMethod> paymentMethodPage = paymentMethodRepository.findAllWithFilter(
                filter.getSearch(),
                filter.getType(),
                filter.getIsActive(),
                pageable
        );

        return paymentMethodMapper.toPaginationResponse(paymentMethodPage, paginationMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMethodResponse getPaymentMethodById(UUID id) {
        PaymentMethod paymentMethod = findPaymentMethodById(id);
        return paymentMethodMapper.toResponse(paymentMethod);
    }

    @Override
    public PaymentMethodResponse updatePaymentMethod(UUID id, PaymentMethodUpdateRequest request) {
        PaymentMethod paymentMethod = findPaymentMethodById(id);

        paymentMethodMapper.updateEntity(request, paymentMethod);
        PaymentMethod updatedPaymentMethod = paymentMethodRepository.save(paymentMethod);

        log.info("Payment method updated successfully: {}", id);
        return paymentMethodMapper.toResponse(updatedPaymentMethod);
    }

    @Override
    public PaymentMethodResponse deletePaymentMethod(UUID id) {
        PaymentMethod paymentMethod = findPaymentMethodById(id);

        paymentMethod.softDelete();
        paymentMethod = paymentMethodRepository.save(paymentMethod);

        log.info("Payment method deleted successfully: {}", id);
        return paymentMethodMapper.toResponse(paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getActivePaymentMethods() {
        List<PaymentMethod> paymentMethods = paymentMethodRepository.findAllActive();
        return paymentMethodMapper.toResponseList(paymentMethods);
    }

    private PaymentMethod findPaymentMethodById(UUID id) {
        return paymentMethodRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new NotFoundException("Payment method not found"));
    }
}
