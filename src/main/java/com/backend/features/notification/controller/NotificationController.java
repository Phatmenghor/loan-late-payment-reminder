package com.backend.features.notification.controller;

import com.backend.config.CpbApiConfig;
import com.backend.features.notification.dto.DebugSignKeyResponse;
import com.backend.features.notification.dto.SendNotificationRequestDto;
import com.backend.features.notification.helper.NotificationPayloadBuilder;
import com.backend.features.notification.helper.SignKeyGenerator;
import com.backend.features.notification.service.NotificationService;
import com.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final SignKeyGenerator signKeyGenerator;
    private final NotificationPayloadBuilder payloadBuilder;
    private final CpbApiConfig cpbApiConfig;
    private final RestTemplate restTemplate;

    @PostMapping("/sms/process-pending")
    public ResponseEntity<ApiResponse<String>> processPendingNotifications() {
        log.info("REST: Processing pending SMS notifications from Oracle");

        notificationService.processPendingSmsNotifications();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Pending SMS notifications are being processed", "ACCEPTED"));
    }

    @PostMapping("/sms/test")
    public ResponseEntity<ApiResponse<String>> sendTestSms(@Valid @RequestBody SendNotificationRequestDto request) {
        log.info("REST: Sending test SMS to {}", request.getPhoneNumber());

        String result = notificationService.sendTestSms(request);
        return ResponseEntity.ok(ApiResponse.success("Test SMS sent successfully", result));
    }

    @PostMapping("/sms/debug")
    public ResponseEntity<ApiResponse<DebugSignKeyResponse>> debugSignKey(@Valid @RequestBody SendNotificationRequestDto request) {
        log.info("REST: Debug sign key for phone: {}", request.getPhoneNumber());

        String phone = request.getPhoneNumber();
        String content = request.getMessageContent();
        String signKey = signKeyGenerator.generateSignKey(phone, content);
        String payload = payloadBuilder.buildJsonPayload(phone, content);
        String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";

        String apiResponse = null;
        String apiCode = null;
        String apiDesc = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(payload, headers), String.class);
            apiResponse = response.getBody();
            log.info("Debug API raw response: {}", apiResponse);
        } catch (Exception e) {
            apiResponse = "ERROR: " + e.getMessage();
            log.error("Debug API call failed: {}", e.getMessage());
        }

        DebugSignKeyResponse debug = DebugSignKeyResponse.builder()
                .phone(phone)
                .signKey(signKey)
                .signKeyFormula("SHA256( KEY + PHONE + CONTENT )")
                .payload(payload)
                .apiUrl(apiUrl)
                .apiResponse(apiResponse)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Debug info", debug));
    }
}
