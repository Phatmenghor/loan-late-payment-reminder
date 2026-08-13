package com.backend.features.sms.controller;

import com.backend.features.sms.dto.BatchProcessingStatus;
import com.backend.features.sms.dto.SendBatchSmsRequest;
import com.backend.features.sms.dto.SendBatchSmsResponse;
import com.backend.features.sms.dto.SendSmsRequest;
import com.backend.features.sms.dto.SendSmsResponse;
import com.backend.features.sms.service.BatchProcessingStatusService;
import com.backend.features.sms.service.SmsService;
import com.backend.shared.dto.ApiResponse;
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
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
@Slf4j
public class SmsController {

    private final SmsService smsService;
    private final BatchProcessingStatusService batchProcessingStatusService;

    @PostMapping("/send-single")
    public ResponseEntity<SendSmsResponse> sendSingleSms(@Valid @RequestBody SendSmsRequest request) {
        log.info("REST request to send single SMS to phone: {}", request.getPhone());
        SendSmsResponse response = smsService.sendSms(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendBatchSmsResponse>> sendBatchSms(@RequestBody(required = false) SendBatchSmsRequest request) {
        log.info("REST request to start batch SMS processing");
        SendBatchSmsResponse response = smsService.startBatchProcessing(request);
        return ResponseEntity.accepted().body(ApiResponse.success("SMS batch processing started successfully", response));
    }

    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<BatchProcessingStatus>> stopBatchProcessing() {
        log.warn("REST request to stop active batch SMS processing");
        BatchProcessingStatus status = smsService.stopBatchProcessing();
        return ResponseEntity.ok(ApiResponse.success("SMS batch processing stopped successfully", status));
    }

    @PostMapping("/status")
    public ResponseEntity<ApiResponse<BatchProcessingStatus>> getProcessingStatus() {
        BatchProcessingStatus status = batchProcessingStatusService.getStatus();
        return ResponseEntity.ok(ApiResponse.success("Batch processing status retrieved", status));
    }
}
