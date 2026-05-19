package com.backend.features.notification.controller;

import com.backend.config.MobileBankingConfig;
import com.backend.features.notification.dto.DebugSignKeyResponse;
import com.backend.features.notification.dto.SendNotificationRequestDto;
import com.backend.features.notification.service.NotificationService;
import com.backend.shared.dto.ApiResponse;
import com.backend.shared.utils.HttpClientUtil;
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
    private final MobileBankingConfig mobileBankingConfig;
    private final HttpClientUtil httpClientUtil;

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
    public ResponseEntity<ApiResponse<DebugSignKeyResponse>> debugSoapSms(@Valid @RequestBody SendNotificationRequestDto request) {
        log.info("REST: Debug SOAP SMS for phone: {}", request.getPhoneNumber());

        String phone = request.getPhoneNumber();
        String content = request.getMessageContent();
        String otpUrl = mobileBankingConfig.getOtpUrl();
        String secretKey = mobileBankingConfig.getSecretKey();
        String requestId = String.valueOf(System.currentTimeMillis());

        String soapXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' "
                + "xmlns:cpb='http://cpbmobile.vnpay.vn'>"
                + "<soap:Header/><soap:Body><cpb:sendSmsNew>"
                + "<cpb:requestId>" + requestId + "</cpb:requestId>"
                + "<cpb:keyword>CPBSMS</cpb:keyword>"
                + "<cpb:mobileNo>" + phone + "</cpb:mobileNo>"
                + "<cpb:content><![CDATA[" + content + "]]></cpb:content>"
                + "<cpb:requestTime></cpb:requestTime>"
                + "<cpb:contentType>9</cpb:contentType>"
                + "<cpb:secretKey>" + secretKey + "</cpb:secretKey>"
                + "</cpb:sendSmsNew></soap:Body></soap:Envelope>";

        String apiResponse;
        try {
            apiResponse = httpClientUtil.postForString(otpUrl, soapXml, "application/soap+xml");
            log.info("Debug SOAP response: {}", apiResponse);
        } catch (Exception e) {
            apiResponse = "ERROR: " + e.getMessage();
            log.error("Debug SOAP call failed: {}", e.getMessage());
        }

        DebugSignKeyResponse debug = DebugSignKeyResponse.builder()
                .phone(phone)
                .content(content)
                .apiUrl(otpUrl)
                .payload(soapXml)
                .apiResponse(apiResponse)
                .build();

        return ResponseEntity.ok(ApiResponse.success("SOAP debug info", debug));
    }
}
