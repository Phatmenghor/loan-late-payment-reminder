package com.backend.features.sms.service;

import com.backend.features.sms.dto.SendBatchSmsResponse;
import com.backend.features.sms.dto.SendSmsRequest;
import com.backend.features.sms.dto.SendSmsResponse;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public interface SmsService {

    SendSmsResponse sendSms(SendSmsRequest request);

    SendBatchSmsResponse startBatchProcessing();

    @Async
    CompletableFuture<SendBatchSmsResponse> processSms();
}
