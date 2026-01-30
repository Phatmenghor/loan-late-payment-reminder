package com.emenu.features.setting.controller;

import com.emenu.features.setting.dto.response.AboutUsResponse;
import com.emenu.features.setting.service.AboutUsService;
import com.emenu.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/about-us")
@RequiredArgsConstructor
@Slf4j
public class PublicAboutUsController {

    private final AboutUsService aboutUsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AboutUsResponse>> getAboutUs() {
        log.info("Getting public about us");
        AboutUsResponse aboutUs = aboutUsService.getAboutUs();
        return ResponseEntity.ok(ApiResponse.success("About Us retrieved successfully", aboutUs));
    }
}
