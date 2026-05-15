package com.backend.features.sms.service;

import com.backend.features.sms.dto.SendBatchSmsResponse;
import com.backend.features.sms.dto.SendSmsRequest;
import com.backend.features.sms.dto.SendSmsResponse;

public interface SmsService {

    SendSmsResponse sendSms(SendSmsRequest request);

    SendBatchSmsResponse processSms();
}
