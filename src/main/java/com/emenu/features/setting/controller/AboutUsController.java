package com.emenu.features.setting.controller;

import com.emenu.features.setting.dto.request.AboutUsCreateRequest;
import com.emenu.features.setting.dto.response.AboutUsResponse;
import com.emenu.features.setting.dto.update.AboutUsUpdateRequest;
import com.emenu.features.setting.service.AboutUsService;
import com.emenu.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/about-us")
@RequiredArgsConstructor
@Slf4j
public class AboutUsController {

    private final AboutUsService aboutUsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AboutUsResponse>> getAboutUs() {
        log.info("Getting about us");
        AboutUsResponse aboutUs = aboutUsService.getAboutUs();
        return ResponseEntity.ok(ApiResponse.success("About Us retrieved successfully", aboutUs));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AboutUsResponse>> createOrUpdate(@Valid @RequestBody AboutUsCreateRequest request) {
        log.info("Creating or updating about us");
        AboutUsResponse aboutUs = aboutUsService.createOrUpdate(request);
        return ResponseEntity.ok(ApiResponse.success("About Us saved successfully", aboutUs));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AboutUsResponse>> update(@Valid @RequestBody AboutUsUpdateRequest request) {
        log.info("Updating about us");
        AboutUsResponse aboutUs = aboutUsService.update(request);
        return ResponseEntity.ok(ApiResponse.success("About Us updated successfully", aboutUs));
    }
}
