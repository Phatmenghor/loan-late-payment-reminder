package com.emenu.features.auth.service;

import com.emenu.features.auth.dto.request.UserLocationCreateRequest;
import com.emenu.features.auth.dto.response.UserLocationResponse;
import com.emenu.features.auth.dto.update.UserLocationUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface UserLocationService {

    UserLocationResponse createLocation(UserLocationCreateRequest request);
    List<UserLocationResponse> getMyLocations();
    UserLocationResponse getLocationById(UUID id);
    UserLocationResponse updateLocation(UUID id, UserLocationUpdateRequest request);
    UserLocationResponse deleteLocation(UUID id);
    UserLocationResponse setPrimary(UUID id);
}
