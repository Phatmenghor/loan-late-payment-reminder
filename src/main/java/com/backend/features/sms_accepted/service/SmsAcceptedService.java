package com.backend.features.sms_accepted.service;

import com.backend.features.sms_accepted.dto.SendAcceptedSmsRequest;
import com.backend.features.sms_accepted.dto.SendAcceptedSmsResponse;

public interface SmsAcceptedService {

    SendAcceptedSmsResponse sendSms(SendAcceptedSmsRequest request);

    void processPendingSmsFromOracle();

    SendAcceptedSmsResponse getStatus(String msgId);
}
