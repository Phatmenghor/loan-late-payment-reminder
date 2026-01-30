package com.emenu.features.auth.mapper;

import com.emenu.features.auth.dto.request.RegisterRequest;
import com.emenu.features.auth.dto.request.UserCreateRequest;
import com.emenu.features.auth.dto.response.LoginResponse;
import com.emenu.features.auth.dto.response.UserBasicInfo;
import com.emenu.features.auth.dto.response.UserResponse;
import com.emenu.features.auth.dto.update.UserUpdateRequest;
import com.emenu.features.auth.models.User;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaginationMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "id", source = "id")
    UserResponse toResponse(User user);

    UserBasicInfo toUserBasicInfo(User user);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "accessToken", source = "token")
    @Mapping(target = "tokenType", constant = "Bearer")
    LoginResponse toLoginResponse(User user, String token);

    List<UserResponse> toResponseList(List<User> users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(UserCreateRequest request);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(RegisterRequest request);

    default PaginationResponse<UserResponse> toPaginationResponse(Page<User> page, PaginationMapper paginationMapper) {
        return paginationMapper.toPaginationResponse(page, this::toResponseList);
    }
}
