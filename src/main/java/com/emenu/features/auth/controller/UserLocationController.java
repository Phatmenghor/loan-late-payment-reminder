package com.emenu.features.auth.controller;

import com.emenu.features.auth.dto.request.UserLocationCreateRequest;
import com.emenu.features.auth.dto.response.UserLocationResponse;
import com.emenu.features.auth.dto.update.UserLocationUpdateRequest;
import com.emenu.features.auth.service.UserLocationService;
import com.emenu.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-locations")
@RequiredArgsConstructor
@Slf4j
public class UserLocationController {

    private final UserLocationService userLocationService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserLocationResponse>> createLocation(@Valid @RequestBody UserLocationCreateRequest request) {
        log.info("Creating user location");
        UserLocationResponse location = userLocationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Location created successfully", location));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserLocationResponse>>> getMyLocations() {
        log.info("Getting user locations");
        List<UserLocationResponse> locations = userLocationService.getMyLocations();
        return ResponseEntity.ok(ApiResponse.success("Locations retrieved successfully", locations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserLocationResponse>> getLocationById(@PathVariable UUID id) {
        log.info("Getting location by ID: {}", id);
        UserLocationResponse location = userLocationService.getLocationById(id);
        return ResponseEntity.ok(ApiResponse.success("Location retrieved successfully", location));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserLocationResponse>> getLocationPrimary() {
        log.info("Getting my primary location");
        UserLocationResponse location = userLocationService.getMyPrimaryLocations();
        return ResponseEntity.ok(ApiResponse.success("Location primary retrieved successfully", location));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserLocationResponse>> updateLocation(
            @PathVariable UUID id,
            @Valid @RequestBody UserLocationUpdateRequest request) {
        log.info("Updating location: {}", id);
        UserLocationResponse location = userLocationService.updateLocation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", location));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UserLocationResponse>> deleteLocation(@PathVariable UUID id) {
        log.info("Deleting location: {}", id);
        UserLocationResponse location = userLocationService.deleteLocation(id);
        return ResponseEntity.ok(ApiResponse.success("Location deleted successfully", location));
    }
}
