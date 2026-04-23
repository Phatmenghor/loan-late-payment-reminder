package com.backend.features.notification.controller;

import com.backend.features.notification.dto.request.SendSmsRequest;
import com.backend.features.notification.dto.response.SendSmsResponse;
import com.backend.features.notification.dto.response.SmsLogResponse;
import com.backend.features.notification.service.NotificationService;
import com.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/sms/send")
    public ResponseEntity<ApiResponse<SendSmsResponse>> sendSms(
            @Valid @RequestBody SendSmsRequest request) {
        log.info("REST: Sending SMS to phone: {}", request.getPhoneNumber());

        SendSmsResponse response = notificationService.sendSms(request);
        return ResponseEntity.ok(ApiResponse.success("SMS sent successfully", response));
    }

    @GetMapping("/sms/logs")
    public ResponseEntity<ApiResponse<List<SmsLogResponse>>> getAllSmsLogs() {
        log.info("REST: Fetching all SMS logs");

        List<SmsLogResponse> logs = notificationService.getAllSmsLogs();
        return ResponseEntity.ok(ApiResponse.success("SMS logs retrieved successfully", logs));
    }

    @GetMapping("/sms/logs/{phoneNumber}")
    public ResponseEntity<ApiResponse<List<SmsLogResponse>>> getSmsLogsByPhone(
            @PathVariable @NotBlank(message = "Phone number is required") String phoneNumber) {
        log.info("REST: Fetching SMS logs for phone: {}", phoneNumber);

        List<SmsLogResponse> logs = notificationService.getSmsLogs(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("SMS logs retrieved successfully", logs));
    }

    @PostMapping("/sms/process-pending")
    public ResponseEntity<ApiResponse<String>> processPendingNotifications() {
        log.info("REST: Processing pending SMS notifications");

        notificationService.processPendingSmsNotifications();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Pending SMS notifications are being processed", "ACCEPTED"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Invalid argument: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        log.error("Unexpected error in notification controller", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
