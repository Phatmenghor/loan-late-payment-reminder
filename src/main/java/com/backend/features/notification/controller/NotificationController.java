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
        String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";
        String key = cpbApiConfig.getEncryptionKey();

        // Show all common sign key formula variants
        java.util.LinkedHashMap<String, String> variants = new java.util.LinkedHashMap<>();
        variants.put("KEY+PHONE+CONTENT",           signKeyGenerator.hashWithSha256(key + phone + content));
        variants.put("PHONE+CONTENT+KEY",           signKeyGenerator.hashWithSha256(phone + content + key));
        variants.put("KEY+CONTENT+PHONE",           signKeyGenerator.hashWithSha256(key + content + phone));
        variants.put("PHONE+KEY+CONTENT",           signKeyGenerator.hashWithSha256(phone + key + content));
        variants.put("CONTENT+PHONE+KEY",           signKeyGenerator.hashWithSha256(content + phone + key));
        variants.put("CONTENT+KEY+PHONE",           signKeyGenerator.hashWithSha256(content + key + phone));
        variants.put("KEY+PHONE",                   signKeyGenerator.hashWithSha256(key + phone));
        variants.put("PHONE+KEY",                   signKeyGenerator.hashWithSha256(phone + key));
        variants.put("KEY+PHONE+KEY+CONTENT+KEY",   signKeyGenerator.hashWithSha256(key + phone + key + content + key));

        log.info("Sign key variants for phone {}: {}", phone, variants);

        String currentSignKey = signKeyGenerator.generateSignKey(phone, content);
        String payload = payloadBuilder.buildJsonPayload(phone, content);

        String apiResponse = null;
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
                .content(content)
                .apiUrl(apiUrl)
                .signKeyVariants(variants)
                .currentFormula("KEY+PHONE+CONTENT")
                .currentSignKey(currentSignKey)
                .payload(payload)
                .apiResponse(apiResponse)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Debug info — share signKeyVariants with API team to identify correct formula", debug));
    }
}
