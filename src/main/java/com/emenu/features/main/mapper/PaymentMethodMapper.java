package com.emenu.features.main.mapper;

import com.emenu.features.main.dto.request.PaymentMethodCreateRequest;
import com.emenu.features.main.dto.response.PaymentMethodResponse;
import com.emenu.features.main.dto.update.PaymentMethodUpdateRequest;
import com.emenu.features.main.models.PaymentMethod;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaginationMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMethodMapper {

    PaymentMethod toEntity(PaymentMethodCreateRequest request);

    PaymentMethodResponse toResponse(PaymentMethod paymentMethod);

    List<PaymentMethodResponse> toResponseList(List<PaymentMethod> paymentMethods);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PaymentMethodUpdateRequest request, @MappingTarget PaymentMethod paymentMethod);

    default PaginationResponse<PaymentMethodResponse> toPaginationResponse(Page<PaymentMethod> paymentMethodPage, PaginationMapper paginationMapper) {
        return paginationMapper.toPaginationResponse(paymentMethodPage, this::toResponseList);
    }
}
