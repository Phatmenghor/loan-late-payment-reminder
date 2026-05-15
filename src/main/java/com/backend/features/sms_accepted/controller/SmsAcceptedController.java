package com.backend.features.sms_accepted.controller;

import com.backend.features.sms_accepted.dto.SendBatchSmsResponse;
import com.backend.features.sms_accepted.dto.SendSmsRequest;
import com.backend.features.sms_accepted.dto.SendSmsResponse;
import com.backend.features.sms_accepted.service.SmsAcceptedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sms-accepted")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SMS Accepted", description = "SMS Accepted Service API")
public class SmsAcceptedController {

    private final SmsAcceptedService smsAcceptedService;

    @PostMapping("/send-single")
    @Operation(summary = "Send Single SMS", description = "Send a single SMS with phone number and content")
    public ResponseEntity<SendSmsResponse> sendSingleSms(@Valid @RequestBody SendSmsRequest request) {
        log.info("API: Sending single SMS - phone: {}", request.getPhone());
        SendSmsResponse response = smsAcceptedService.sendSms(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/send")
    @Operation(summary = "Process and Send Batch SMS", description = "Process PROCESSING records from Oracle D_CBS_SMS_LOG and send SMS via SOAP gateway")
    public ResponseEntity<SendBatchSmsResponse> sendBatchSms() {
        log.info("API: Processing batch SMS from Oracle D_CBS_SMS_LOG");
        SendBatchSmsResponse response = smsAcceptedService.processSms();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
