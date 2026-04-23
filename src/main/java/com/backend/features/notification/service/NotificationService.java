package com.backend.features.notification.service;

import com.backend.features.notification.dto.request.SendSmsRequest;
import com.backend.features.notification.dto.response.SendSmsResponse;
import com.backend.features.notification.dto.response.SmsLogResponse;

import java.util.List;

public interface NotificationService {

    void processPendingSmsNotifications();

    SendSmsResponse sendSms(SendSmsRequest request);

    List<SmsLogResponse> getSmsLogs(String phoneNumber);

    List<SmsLogResponse> getAllSmsLogs();
}
