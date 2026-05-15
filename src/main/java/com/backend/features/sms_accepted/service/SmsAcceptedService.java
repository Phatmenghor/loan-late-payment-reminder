package com.backend.features.sms_accepted.service;

import com.backend.features.sms_accepted.dto.SendBatchSmsResponse;

public interface SmsAcceptedService {

    SendBatchSmsResponse processSms();
}
