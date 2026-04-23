package com.backend.features.notification.service.impl;

import com.backend.features.notification.repository.NotificationSettingRepository;
import com.backend.features.notification.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingServiceImpl implements SettingService {

    private static final String DEFAULT_MESSAGE_KEY = "default_message_content";

    private final NotificationSettingRepository settingRepository;

    @Override
    public String getSettingValueByKey(String key) {
        log.debug("Fetching setting value for key: {}", key);
        return settingRepository.findByKey(key)
                .map(setting -> {
                    log.debug("Setting found for key: {}", key);
                    return setting.getValue() != null ? setting.getValue() : "";
                })
                .orElse("");
    }

    @Override
    public String getDefaultMessageContent() {
        log.debug("Fetching default message content");
        String content = getSettingValueByKey(DEFAULT_MESSAGE_KEY);
        if (content.isEmpty()) {
            log.warn("Default message content not found, using empty string");
        }
        return content;
    }
}
