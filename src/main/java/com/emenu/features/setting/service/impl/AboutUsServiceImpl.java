package com.emenu.features.setting.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.features.setting.dto.request.AboutUsCreateRequest;
import com.emenu.features.setting.dto.response.AboutUsResponse;
import com.emenu.features.setting.dto.update.AboutUsUpdateRequest;
import com.emenu.features.setting.mapper.AboutUsMapper;
import com.emenu.features.setting.models.AboutUs;
import com.emenu.features.setting.repository.AboutUsRepository;
import com.emenu.features.setting.service.AboutUsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AboutUsServiceImpl implements AboutUsService {

    private final AboutUsRepository aboutUsRepository;
    private final AboutUsMapper aboutUsMapper;

    @Override
    @Transactional(readOnly = true)
    public AboutUsResponse getAboutUs() {
        AboutUs aboutUs = aboutUsRepository.findFirstByIsDeletedFalseOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException("About Us not found"));
        return aboutUsMapper.toResponse(aboutUs);
    }

    @Override
    public AboutUsResponse createOrUpdate(AboutUsCreateRequest request) {
        Optional<AboutUs> existing = aboutUsRepository.findFirstByIsDeletedFalseOrderByCreatedAtAsc();

        AboutUs aboutUs;
        if (existing.isPresent()) {
            aboutUs = existing.get();
            aboutUs.setTitle(request.getTitle());
            aboutUs.setDescription(request.getDescription());
            aboutUs.setMainImageUrl(request.getMainImageUrl());
            aboutUs.setContactInfo(request.getContactInfo());
            aboutUs.setShowroomHours(request.getShowroomHours());
            aboutUs.setPhoneNumber(request.getPhoneNumber());
            aboutUs.setEmail(request.getEmail());
            aboutUs.setAddress(request.getAddress());
            aboutUs.setFacebookUrl(request.getFacebookUrl());
            aboutUs.setInstagramUrl(request.getInstagramUrl());
            aboutUs.setWebsiteUrl(request.getWebsiteUrl());
            aboutUs.setLatitude(request.getLatitude());
            aboutUs.setLongitude(request.getLongitude());
            if (request.getStatus() != null) {
                aboutUs.setStatus(request.getStatus());
            }
            log.info("About Us updated (create or update)");
        } else {
            aboutUs = aboutUsMapper.toEntity(request);
            log.info("About Us created");
        }

        AboutUs saved = aboutUsRepository.save(aboutUs);
        return aboutUsMapper.toResponse(saved);
    }

    @Override
    public AboutUsResponse update(AboutUsUpdateRequest request) {
        AboutUs aboutUs = aboutUsRepository.findFirstByIsDeletedFalseOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException("About Us not found"));

        aboutUsMapper.updateEntity(request, aboutUs);
        AboutUs updated = aboutUsRepository.save(aboutUs);

        log.info("About Us updated");
        return aboutUsMapper.toResponse(updated);
    }
}
