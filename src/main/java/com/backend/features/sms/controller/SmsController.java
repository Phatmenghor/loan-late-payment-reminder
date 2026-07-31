package com.backend.features.sms.controller;

import com.backend.features.sms.dto.BatchProcessingStatus;
import com.backend.features.sms.dto.SendBatchSmsRequest;
import com.backend.features.sms.dto.SendBatchSmsResponse;
import com.backend.features.sms.dto.SendSmsRequest;
import com.backend.features.sms.dto.SendSmsResponse;
import com.backend.features.sms.service.BatchProcessingStatusService;
import com.backend.features.sms.service.SmsService;
import com.backend.shared.dto.ApiResponse;
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
        log.info("REST request to send single SMS to phone: {}", request.getPhone());
        SendSmsResponse response = smsService.sendSms(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/send")
    @Operation(
        summary = "Process and Send Batch SMS",
        description = "Asynchronously process batch SMS. Supports optional request body with custom message content, explicit phone numbers list, or custom items list without reading DB table."
    )
    public ResponseEntity<ApiResponse<SendBatchSmsResponse>> sendBatchSms(@RequestBody(required = false) SendBatchSmsRequest request) {
        log.info("REST request to start batch SMS processing");
        SendBatchSmsResponse response = smsService.startBatchProcessing(request);
        return ResponseEntity.accepted().body(ApiResponse.success("SMS batch processing started successfully", response));
    }

    @PostMapping("/stop")
    @Operation(
        summary = "Stop / Cancel Batch SMS Processing",
        description = "Gracefully interrupts and stops the active SMS batch processing loop and releases resources"
    )
    public ResponseEntity<ApiResponse<BatchProcessingStatus>> stopBatchProcessing() {
        log.warn("REST request to stop active batch SMS processing");
        BatchProcessingStatus status = smsService.stopBatchProcessing();
        return ResponseEntity.ok(ApiResponse.success("SMS batch processing stopped successfully", status));
    }

    @GetMapping("/status")
    @Operation(summary = "Get Batch SMS Processing Status", description = "Check current progress of batch SMS processing including percentage complete, total sent, and status")
    public ResponseEntity<ApiResponse<BatchProcessingStatus>> getProcessingStatus() {
        BatchProcessingStatus status = batchProcessingStatusService.getStatus();
        return ResponseEntity.ok(ApiResponse.success("Batch processing status retrieved", status));
    }
}
