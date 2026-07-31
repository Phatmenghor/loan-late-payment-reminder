package com.backend.features.notification.controller;

import com.backend.features.notification.dto.SendNotificationRequestDto;
import com.backend.features.notification.service.NotificationService;
import com.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/sms/test")
    public ResponseEntity<ApiResponse<String>> sendTestSms(@Valid @RequestBody SendNotificationRequestDto request) {
        log.info("REST: Sending test SMS to {}", request.getPhoneNumber());

        String result = notificationService.sendTestSms(request);
        return ResponseEntity.ok(ApiResponse.success("Test SMS sent successfully", result));
    }
}
