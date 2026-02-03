package com.emenu.features.auth.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.features.auth.dto.request.UserLocationCreateRequest;
import com.emenu.features.auth.dto.response.UserLocationResponse;
import com.emenu.features.auth.dto.update.UserLocationUpdateRequest;
import com.emenu.features.auth.mapper.UserLocationMapper;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.models.UserLocation;
import com.emenu.features.auth.repository.UserLocationRepository;
import com.emenu.features.auth.service.UserLocationService;
import com.emenu.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserLocationServiceImpl implements UserLocationService {

    private final UserLocationRepository userLocationRepository;
    private final UserLocationMapper userLocationMapper;
    private final SecurityUtils securityUtils;

    @Override
    public UserLocationResponse createLocation(UserLocationCreateRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        UserLocation location = userLocationMapper.toEntity(request);
        location.setUserId(currentUser.getId());

        long count = userLocationRepository.countByUserId(currentUser.getId());
        if (count == 0) {
            location.setIsPrimary(true);
        } else if (Boolean.TRUE.equals(request.getIsPrimary())) {
            userLocationRepository.clearPrimaryByUserId(currentUser.getId());
            location.setIsPrimary(true);
        } else {
            location.setIsPrimary(false);
        }

        UserLocation saved = userLocationRepository.save(location);
        log.info("User location created: {} for user: {}", saved.getId(), currentUser.getId());
        return userLocationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLocationResponse> getMyLocations() {
        User currentUser = securityUtils.getCurrentUser();
        List<UserLocation> locations = userLocationRepository.findByUserId(currentUser.getId());
        return userLocationMapper.toResponseList(locations);
    }

    @Override
    public UserLocationResponse getMyPrimaryLocations() {
        User currentUser = securityUtils.getCurrentUser();
        UserLocation location = userLocationRepository.findPrimaryByUserId(currentUser.getId()).orElseThrow(() -> new NotFoundException("Location not found"));
        return userLocationMapper.toResponse(location);
    }

    @Override
    @Transactional(readOnly = true)
    public UserLocationResponse getLocationById(UUID id) {
        User currentUser = securityUtils.getCurrentUser();
        UserLocation location = userLocationRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Location not found"));
        return userLocationMapper.toResponse(location);
    }

    @Override
    public UserLocationResponse updateLocation(UUID id, UserLocationUpdateRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        UserLocation location = userLocationRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Location not found"));

        if (Boolean.TRUE.equals(request.getIsPrimary()) && !Boolean.TRUE.equals(location.getIsPrimary())) {
            userLocationRepository.clearPrimaryByUserId(currentUser.getId());
        }

        userLocationMapper.updateEntity(request, location);
        UserLocation updated = userLocationRepository.save(location);

        log.info("User location updated: {}", id);
        return userLocationMapper.toResponse(updated);
    }

    @Override
    public UserLocationResponse deleteLocation(UUID id) {
        User currentUser = securityUtils.getCurrentUser();

        UserLocation location = userLocationRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Location not found"));

        location.softDelete();
        UserLocation deleted = userLocationRepository.save(location);

        if (Boolean.TRUE.equals(location.getIsPrimary())) {
            List<UserLocation> remaining = userLocationRepository.findByUserId(currentUser.getId());
            if (!remaining.isEmpty()) {
                UserLocation newPrimary = remaining.getFirst();
                newPrimary.setIsPrimary(true);
                userLocationRepository.save(newPrimary);
            }
        }

        log.info("User location deleted: {}", id);
        return userLocationMapper.toResponse(deleted);
    }
}
