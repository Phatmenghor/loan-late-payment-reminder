package com.emenu.features.main.mapper;

import com.emenu.features.main.dto.response.CartItemResponse;
import com.emenu.features.main.models.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartItemMapper {

    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.mainImageUrl", target = "productMainImageUrl")
    @Mapping(source = "productSize.name", target = "productSizeName")
    @Mapping(target = "totalPrice", expression = "java(cartItem.getTotalPrice())")
    CartItemResponse toResponse(CartItem cartItem);

    List<CartItemResponse> toResponseList(List<CartItem> cartItems);
}
