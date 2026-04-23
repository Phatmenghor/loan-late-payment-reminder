package com.backend.features.notification.service;

public interface SettingService {

    String getSettingValueByKey(String key);

    String getDefaultMessageContent();
}
