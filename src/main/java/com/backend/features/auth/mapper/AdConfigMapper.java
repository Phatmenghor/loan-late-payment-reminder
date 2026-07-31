package com.backend.features.auth.mapper;

import com.backend.features.auth.dto.AdConfigResponse;
import com.backend.features.auth.model.AdConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdConfigMapper {

    AdConfigResponse toResponse(AdConfig entity);
}
