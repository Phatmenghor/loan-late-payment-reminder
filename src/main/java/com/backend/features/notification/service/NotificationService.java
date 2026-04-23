package com.backend.features.notification.service;

import com.backend.features.notification.dto.SendSmsRequestDto;
import com.backend.features.notification.enums.ProcessingResult;

public interface NotificationService {

    ProcessingResult processPendingSmsNotifications();

    String sendTestSms(SendSmsRequestDto request);
}
