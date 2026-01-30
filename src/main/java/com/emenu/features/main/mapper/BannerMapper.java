package com.emenu.features.main.mapper;

import com.emenu.features.main.dto.request.BannerCreateRequest;
import com.emenu.features.main.dto.response.BannerResponse;
import com.emenu.features.main.dto.update.BannerUpdateRequest;
import com.emenu.features.main.models.Banner;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaginationMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BannerMapper {

    Banner toEntity(BannerCreateRequest request);

    BannerResponse toResponse(Banner banner);

    List<BannerResponse> toResponseList(List<Banner> banners);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(BannerUpdateRequest request, @MappingTarget Banner banner);

    default PaginationResponse<BannerResponse> toPaginationResponse(Page<Banner> bannerPage, PaginationMapper paginationMapper) {
        return paginationMapper.toPaginationResponse(bannerPage, this::toResponseList);
    }
}
