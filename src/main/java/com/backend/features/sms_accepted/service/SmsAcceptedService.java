package com.backend.features.sms_accepted.service;

import com.backend.features.sms_accepted.dto.SendBatchSmsResponse;
import com.backend.features.sms_accepted.dto.SendSmsRequest;
import com.backend.features.sms_accepted.dto.SendSmsResponse;

public interface SmsAcceptedService {

    SendSmsResponse sendSms(SendSmsRequest request);

    SendBatchSmsResponse processSms();
}
