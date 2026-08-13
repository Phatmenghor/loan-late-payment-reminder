package com.backend.features.auth.service;

import com.backend.features.auth.dto.response.AdUserDto;

import java.util.Map;

public interface AdAuthAsyncService {

    void saveOrUpdateUserCacheAsync(String traceId, String username, String rawPassword, AdUserDto adUserDto, Map<String, Object> attrs);

    void saveOrUpdateSystemUserAsync(String traceId, String apiKey, String username);

    void saveAdLogAsync(String traceId, String clientIp, String apiKey, String username, boolean success, String failureReason, Map<String, Object> attrs);
}
