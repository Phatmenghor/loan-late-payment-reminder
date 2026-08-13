package com.backend.features.auth.mapper;

import com.backend.features.auth.dto.response.AdSystemUserResponse;
import com.backend.features.auth.model.AdSystemUser;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdSystemUserMapper {

    AdSystemUserResponse toResponse(AdSystemUser entity);

    List<AdSystemUserResponse> toResponseList(List<AdSystemUser> entities);
}
