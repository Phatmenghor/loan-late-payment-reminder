package com.emenu.features.setting.mapper;

import com.emenu.features.setting.dto.request.AboutUsCreateRequest;
import com.emenu.features.setting.dto.response.AboutUsResponse;
import com.emenu.features.setting.dto.update.AboutUsUpdateRequest;
import com.emenu.features.setting.models.AboutUs;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AboutUsMapper {

    AboutUs toEntity(AboutUsCreateRequest request);

    AboutUsResponse toResponse(AboutUs aboutUs);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(AboutUsUpdateRequest request, @MappingTarget AboutUs aboutUs);
}
