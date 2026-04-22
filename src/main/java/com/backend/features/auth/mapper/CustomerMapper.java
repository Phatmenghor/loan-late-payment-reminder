package com.backend.features.auth.mapper;

import com.backend.features.auth.dto.request.UserCreateRequest;
import com.backend.features.auth.dto.response.UserResponse;
import com.backend.features.auth.dto.update.UserUpdateRequest;
import com.backend.features.auth.models.User;
import com.backend.shared.dto.PaginationResponse;
import com.backend.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaginationMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    @Mapping(target = "userType", constant = "CUSTOMER")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "accountStatus", constant = "ACTIVE")
    User toEntity(UserCreateRequest request);

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "userType", ignore = true)
    @Mapping(target = "accountStatus", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);

    /**
     * Restricted update for current customer profile - only allows safe fields
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "userType", ignore = true)
    @Mapping(target = "accountStatus", ignore = true)
    void updateCurrentUserProfile(UserUpdateRequest request, @MappingTarget User user);

    default PaginationResponse<UserResponse> toPaginationResponse(Page<User> customerPage, PaginationMapper paginationMapper) {
return paginationMapper.toPaginationResponse(customerPage, this::toResponseList);
    }
}