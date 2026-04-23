package com.backend.features.notification.service;

import com.backend.features.notification.dto.SendSmsRequestDto;

public interface NotificationService {

    void processPendingSmsNotifications();

    String sendTestSms(SendSmsRequestDto request);
}
