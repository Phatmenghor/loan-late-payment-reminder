package com.backend.features.auth.service;

import com.backend.features.auth.dto.AdUserDto;

import java.util.Map;

public interface AdAuthAsyncService {

    void saveOrUpdateUserCacheAsync(String username, String rawPassword, AdUserDto adUserDto, Map<String, Object> attrs);

    void saveAdLogAsync(String traceId, String clientIp, String appName, String apiKey, String username, boolean success, String failureReason, Map<String, Object> attrs);
}
