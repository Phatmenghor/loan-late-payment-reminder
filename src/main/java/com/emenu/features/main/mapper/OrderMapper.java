package com.emenu.features.main.mapper;

import com.emenu.features.auth.mapper.UserLocationMapper;
import com.emenu.features.main.dto.response.OrderItemResponse;
import com.emenu.features.main.dto.response.OrderResponse;
import com.emenu.features.main.models.Order;
import com.emenu.features.main.models.OrderItem;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaginationMapper.class, PaymentMethodMapper.class, UserLocationMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "statusDescription", expression = "java(order.getStatus().getDescription())")
    @Mapping(target = "customerName", expression = "java(order.getUser() != null ? order.getUser().getFullName() : null)")
    @Mapping(target = "customerPhone", expression = "java(order.getUser() != null ? order.getUser().getPhoneNumber() : null)")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

    List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> orderItems);

    default PaginationResponse<OrderResponse> toPaginationResponse(Page<Order> orderPage, PaginationMapper paginationMapper) {
        return paginationMapper.toPaginationResponse(orderPage, this::toResponseList);
    }
}
