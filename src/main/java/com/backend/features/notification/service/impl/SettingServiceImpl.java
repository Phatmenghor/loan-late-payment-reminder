package com.backend.features.notification.service.impl;

import com.backend.features.notification.repository.SettingRepository;
import com.backend.features.notification.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;

    @Override
    public String getSettingDescription() {
        return settingRepository.findAll()
                .stream()
                .findFirst()
                .map(setting -> setting.getDescription() != null ? setting.getDescription() : "")
                .orElse("");
    }
}
