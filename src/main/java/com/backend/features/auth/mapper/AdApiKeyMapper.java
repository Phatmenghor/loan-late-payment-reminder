package com.backend.features.auth.mapper;

import com.backend.features.auth.dto.AdApiKeyResponse;
import com.backend.features.auth.model.AdApiKey;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdApiKeyMapper {

    AdApiKeyResponse toResponse(AdApiKey entity);

    List<AdApiKeyResponse> toResponseList(List<AdApiKey> entities);
}
