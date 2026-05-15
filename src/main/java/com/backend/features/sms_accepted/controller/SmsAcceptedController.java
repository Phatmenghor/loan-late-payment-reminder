package com.backend.features.sms_accepted.controller;

import com.backend.features.sms_accepted.dto.SendAcceptedSmsRequest;
import com.backend.features.sms_accepted.dto.SendAcceptedSmsResponse;
import com.backend.features.sms_accepted.service.SmsAcceptedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sms-accepted")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SMS Accepted", description = "SMS Accepted Service API")
public class SmsAcceptedController {

    private final SmsAcceptedService smsAcceptedService;

    @PostMapping("/send")
    @Operation(summary = "Send SMS via SOAP Gateway", description = "Send SMS message to a phone number")
    public ResponseEntity<SendAcceptedSmsResponse> sendSms(@RequestBody SendAcceptedSmsRequest request) {
        log.info("API: SMS send request - phone: {}", request.getPhone());
        SendAcceptedSmsResponse response = smsAcceptedService.sendSms(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/process-pending")
    @Operation(summary = "Process Pending SMS from Oracle", description = "Pull and process pending SMS records from Oracle VIEW_SMS")
    public ResponseEntity<String> processPending() {
        log.info("API: Processing pending SMS from Oracle VIEW_SMS");
        smsAcceptedService.processPendingSmsFromOracle();
        return ResponseEntity.ok("Processing pending SMS from Oracle started");
    }

    @GetMapping("/status/{msgId}")
    @Operation(summary = "Get SMS Status", description = "Get the current status of an SMS by message ID")
    public ResponseEntity<SendAcceptedSmsResponse> getStatus(@PathVariable String msgId) {
        log.info("API: Fetching SMS status - msgId: {}", msgId);
        SendAcceptedSmsResponse response = smsAcceptedService.getStatus(msgId);
        return ResponseEntity.ok(response);
    }
}
