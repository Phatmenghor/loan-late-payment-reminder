package com.backend.features.notification.controller;

import com.backend.features.notification.service.NotificationService;
import com.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
