package com.backend.features.sms.controller;

import com.backend.features.sms.dto.BatchProcessingStatus;
import com.backend.features.sms.dto.SendBatchSmsResponse;
import com.backend.features.sms.dto.SendSmsRequest;
import com.backend.features.sms.dto.SendSmsResponse;
import com.backend.features.sms.service.BatchProcessingStatusService;
import com.backend.features.sms.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SMS", description = "SMS Service API")
public class SmsController {

    private final SmsService smsService;
    private final BatchProcessingStatusService batchProcessingStatusService;

    @PostMapping("/send-single")
    @Operation(summary = "Send Single SMS", description = "Send a single SMS with phone number and content")
    public ResponseEntity<SendSmsResponse> sendSingleSms(@Valid @RequestBody SendSmsRequest request) {
        SendSmsResponse response = smsService.sendSms(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/send")
    @Operation(summary = "Process and Send Batch SMS (Async - No Timeout)", description = "Asynchronously process PROCESSING records from Oracle D_CBS_SMS_LOG and send SMS via SOAP gateway. Supports up to 100,000+ records without timeout.")
    public ResponseEntity<SendBatchSmsResponse> sendBatchSms() {
        SendBatchSmsResponse response = smsService.startBatchProcessing();
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Get Batch SMS Processing Status", description = "Check current progress of batch SMS processing including percentage complete, total sent, and success count")
    public ResponseEntity<BatchProcessingStatus> getProcessingStatus() {
        BatchProcessingStatus status = batchProcessingStatusService.getStatus();
        return ResponseEntity.ok(status);
    }
}
