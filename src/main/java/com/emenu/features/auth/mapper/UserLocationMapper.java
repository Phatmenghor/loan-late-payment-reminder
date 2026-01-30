package com.emenu.features.auth.mapper;

import com.emenu.features.auth.dto.request.UserLocationCreateRequest;
import com.emenu.features.auth.dto.response.UserLocationResponse;
import com.emenu.features.auth.dto.update.UserLocationUpdateRequest;
import com.emenu.features.auth.models.UserLocation;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserLocationMapper {

    @Mapping(target = "user", ignore = true)
    UserLocation toEntity(UserLocationCreateRequest request);

    UserLocationResponse toResponse(UserLocation userLocation);

    List<UserLocationResponse> toResponseList(List<UserLocation> userLocations);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    void updateEntity(UserLocationUpdateRequest request, @MappingTarget UserLocation userLocation);
}
