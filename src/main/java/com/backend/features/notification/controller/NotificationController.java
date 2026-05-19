package com.backend.features.notification.controller;

import com.backend.enums.common.ProcessingResult;
import com.backend.features.notification.dto.SendNotificationRequestDto;
import com.backend.features.notification.service.NotificationService;
import com.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/sms/process-pending")
    public ResponseEntity<ApiResponse<String>> processPendingNotifications() {
        log.info("REST: Processing pending SMS notifications from Oracle");

        notificationService.processPendingSmsNotifications();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Pending SMS notifications are being processed", "ACCEPTED"));
    }

    @PostMapping("/sms/run-now")
    public ResponseEntity<ApiResponse<String>> runScheduleNow() {
        log.info("REST: Manual trigger - running scheduled SMS processing");

        ProcessingResult result = notificationService.processPendingSmsNotifications();
        return ResponseEntity.ok(ApiResponse.success("Processing complete", result.getDescription()));
    }

    @PostMapping("/sms/test")
    public ResponseEntity<ApiResponse<String>> sendTestSms(@Valid @RequestBody SendNotificationRequestDto request) {
        log.info("REST: Sending test SMS to {}", request.getPhoneNumber());

        String result = notificationService.sendTestSms(request);
        return ResponseEntity.ok(ApiResponse.success("Test SMS sent successfully", result));
    }
}
