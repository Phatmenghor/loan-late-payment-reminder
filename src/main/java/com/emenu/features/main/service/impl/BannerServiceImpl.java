package com.emenu.features.main.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.auth.models.User;
import com.emenu.features.main.dto.filter.BannerFilterRequest;
import com.emenu.features.main.dto.filter.BannerAllFilterRequest;
import com.emenu.features.main.dto.request.BannerCreateRequest;
import com.emenu.features.main.dto.response.BannerResponse;
import com.emenu.features.main.dto.update.BannerUpdateRequest;
import com.emenu.features.main.mapper.BannerMapper;
import com.emenu.features.main.models.Banner;
import com.emenu.features.main.repository.BannerRepository;
import com.emenu.features.main.service.BannerService;
import com.emenu.security.SecurityUtils;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import com.emenu.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final SecurityUtils securityUtils;
    private final PaginationMapper paginationMapper;

    @Override
    public BannerResponse createBanner(BannerCreateRequest request) {
        log.info("Creating banner for current user's business");

        Banner banner = bannerMapper.toEntity(request);

        Banner savedBanner = bannerRepository.save(banner);

        log.info("Banner created successfully: {}", savedBanner.getId());
        return bannerMapper.toResponse(savedBanner);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BannerResponse> getAllBanners(BannerFilterRequest filter) {
        Pageable pageable = PaginationUtils.createPageable(
                filter.getPageNo(), filter.getPageSize(), filter.getSortBy(), filter.getSortDirection()
        );

        Page<Banner> bannerPage = bannerRepository.findAllWithFilters(
                filter.getStatus(),
                filter.getSearch(),
                pageable
        );
        return bannerMapper.toPaginationResponse(bannerPage, paginationMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllItemBanners(BannerAllFilterRequest filter) {
        List<Banner> banners = bannerRepository.findAllListWithFilters(
                filter.getStatus(),
                filter.getSearch(),
                PaginationUtils.createSort(filter.getSortBy(), filter.getSortDirection())
        );
        return bannerMapper.toResponseList(banners);
    }


    @Override
    @Transactional(readOnly = true)
    public BannerResponse getBannerById(UUID id) {
        Banner banner = bannerRepository.findByIdWithBusiness(id)
                .orElseThrow(() -> new NotFoundException("Banner not found"));

        return bannerMapper.toResponse(banner);
    }

    @Override
    public BannerResponse updateBanner(UUID id, BannerUpdateRequest request) {
        Banner banner = bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Banner not found"));

        bannerMapper.updateEntity(request, banner);
        Banner updatedBanner = bannerRepository.save(banner);

        log.info("Banner updated successfully: {}", id);
        return bannerMapper.toResponse(updatedBanner);
    }

    @Override
    public BannerResponse deleteBanner(UUID id) {
        Banner banner = bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Banner not found"));

        banner.softDelete();
        banner = bannerRepository.save(banner);

        log.info("Banner deleted successfully: {}", id);
        return bannerMapper.toResponse(banner);
    }

}