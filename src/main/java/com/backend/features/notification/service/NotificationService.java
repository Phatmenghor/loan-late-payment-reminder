package com.backend.features.notification.service;

import com.backend.features.notification.dto.SendNotificationRequestDto;
import com.backend.features.notification.enums.ProcessingResult;

public interface NotificationService {

    ProcessingResult processPendingSmsNotifications();

    String sendTestSms(SendNotificationRequestDto request);
}
