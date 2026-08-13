package com.backend.features.auth.service.impl;

import com.backend.enums.common.AdUserStatus;
import com.backend.features.auth.dto.response.AdUserDto;
import com.backend.features.auth.model.AdLog;
import com.backend.features.auth.model.AdSystemUser;
import com.backend.features.auth.model.AdUserCache;
import com.backend.features.auth.repository.AdLogRepository;
import com.backend.features.auth.repository.AdSystemUserRepository;
import com.backend.features.auth.repository.AdUserCacheRepository;
import com.backend.features.auth.service.AdAuthAsyncService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdAuthAsyncServiceImpl implements AdAuthAsyncService {

    private final AdUserCacheRepository adUserCacheRepository;
    private final AdSystemUserRepository adSystemUserRepository;
    private final AdLogRepository adLogRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final ConcurrentLinkedQueue<UserCacheBatchItem> userCacheQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SystemUserBatchItem> systemUserQueue = new ConcurrentLinkedQueue<>();

    private static final int BATCH_FLUSH_THRESHOLD = 50;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class UserCacheBatchItem {
        private String traceId;
        private String username;
        private String rawPassword;
        private AdUserDto adUserDto;
        private Map<String, Object> attrs;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SystemUserBatchItem {
        private String traceId;
        private String apiKey;
        private String username;
        private LocalDateTime timestamp;
    }

    @Override
    @Async
    public void saveOrUpdateUserCacheAsync(String traceId, String username, String rawPassword, AdUserDto adUserDto, Map<String, Object> attrs) {
        if (username == null || username.isBlank()) {
            return;
        }
        UserCacheBatchItem item = UserCacheBatchItem.builder()
                .traceId(traceId)
                .username(sanitizeUtf8(username))
                .rawPassword(rawPassword)
                .adUserDto(adUserDto)
                .attrs(attrs)
                .timestamp(LocalDateTime.now())
                .build();

        userCacheQueue.offer(item);
        log.info("ASYNC: Queued user cache update for: {} (Queue size: {})", username, userCacheQueue.size());

        if (userCacheQueue.size() >= BATCH_FLUSH_THRESHOLD) {
            flushUserCacheQueue();
        }
    }

    @Override
    @Async
    public void saveOrUpdateSystemUserAsync(String traceId, String apiKey, String username) {
        if (username == null || username.isBlank() || apiKey == null || apiKey.isBlank()) {
            return;
        }
        SystemUserBatchItem item = SystemUserBatchItem.builder()
                .traceId(traceId)
                .apiKey(sanitizeUtf8(apiKey))
                .username(sanitizeUtf8(username))
                .timestamp(LocalDateTime.now())
                .build();

        systemUserQueue.offer(item);
        log.info("ASYNC: Queued system user update for: {} under API key (Queue size: {})", username, systemUserQueue.size());

        if (systemUserQueue.size() >= BATCH_FLUSH_THRESHOLD) {
            flushSystemUserQueue();
        }
    }

    /**
     * Periodic background flusher running every 2 seconds to flush queued updates in batches.
     */
    @Scheduled(fixedDelay = 2000)
    public void flushQueuesPeriodically() {
        if (!userCacheQueue.isEmpty()) {
            flushUserCacheQueue();
        }
        if (!systemUserQueue.isEmpty()) {
            flushSystemUserQueue();
        }
    }

    @Transactional
    public synchronized void flushUserCacheQueue() {
        if (userCacheQueue.isEmpty()) {
            return;
        }

        List<UserCacheBatchItem> batch = new ArrayList<>();
        UserCacheBatchItem item;
        while ((item = userCacheQueue.poll()) != null && batch.size() < 100) {
            batch.add(item);
        }

        if (!batch.isEmpty()) {
            try {
                processUserCacheBatch(batch);
                log.info("ASYNC BATCH: Successfully flushed {} user cache updates to database", batch.size());
            } catch (Exception e) {
                log.error("ASYNC BATCH: Error flushing user cache batch: {}", e.getMessage(), e);
            }
        }
    }

    @Transactional
    public synchronized void flushSystemUserQueue() {
        if (systemUserQueue.isEmpty()) {
            return;
        }

        List<SystemUserBatchItem> batch = new ArrayList<>();
        SystemUserBatchItem item;
        while ((item = systemUserQueue.poll()) != null && batch.size() < 100) {
            batch.add(item);
        }

        if (!batch.isEmpty()) {
            try {
                processSystemUserBatch(batch);
                log.info("ASYNC BATCH: Successfully flushed {} system user updates to database", batch.size());
            } catch (Exception e) {
                log.error("ASYNC BATCH: Error flushing system user batch: {}", e.getMessage(), e);
            }
        }
    }

    private void processUserCacheBatch(List<UserCacheBatchItem> items) {
        Map<String, AdUserCache> cacheMap = new LinkedHashMap<>();

        for (UserCacheBatchItem item : items) {
            String cleanUsername = sanitizeUtf8(item.getUsername());
            if (cleanUsername == null || cleanUsername.isBlank()) {
                continue;
            }

            String encryptedPassword = item.getRawPassword() != null ? passwordEncoder.encode(item.getRawPassword()) : null;
            String attrsJson = serializeAttrs(item.getAttrs(), cleanUsername);
            String memberOfJson = serializeMemberOf(item.getAttrs(), cleanUsername);

            AdUserCache userCache = cacheMap.computeIfAbsent(cleanUsername, u ->
                    adUserCacheRepository.findByUsernameAndIsDeletedFalse(u)
                            .orElseGet(() -> AdUserCache.builder().username(u).encryptedPassword("").build())
            );

            if (encryptedPassword != null) {
                userCache.setEncryptedPassword(encryptedPassword);
            }
            userCache.copyProfileFrom(item.getAdUserDto(), attrsJson);
            if (memberOfJson != null) {
                userCache.setMemberOf(memberOfJson);
            }
            userCache.setLastLoginAt(item.getTimestamp() != null ? item.getTimestamp() : LocalDateTime.now());
        }

        if (!cacheMap.isEmpty()) {
            adUserCacheRepository.saveAll(cacheMap.values());
        }
    }

    private void processSystemUserBatch(List<SystemUserBatchItem> items) {
        Map<String, AdSystemUser> systemUserMap = new LinkedHashMap<>();

        for (SystemUserBatchItem item : items) {
            String cleanApiKey = sanitizeUtf8(item.getApiKey());
            String cleanUsername = sanitizeUtf8(item.getUsername());

            if (cleanApiKey == null || cleanUsername == null) {
                continue;
            }

            String compositeKey = cleanApiKey + "#" + cleanUsername;

            AdSystemUser systemUser = systemUserMap.computeIfAbsent(compositeKey, k ->
                    adSystemUserRepository.findByApiKeyAndUsername(cleanApiKey, cleanUsername)
                            .orElseGet(() -> AdSystemUser.builder()
                                    .apiKey(cleanApiKey)
                                    .username(cleanUsername)
                                    .status(AdUserStatus.ACTIVE)
                                    .build())
            );

            systemUser.setStatus(AdUserStatus.ACTIVE);
            systemUser.setLastLoginAt(item.getTimestamp() != null ? item.getTimestamp() : LocalDateTime.now());
            systemUser.setLockReason(null);
            systemUser.setLockedAt(null);
        }

        if (!systemUserMap.isEmpty()) {
            adSystemUserRepository.saveAll(systemUserMap.values());
        }
    }

    @Override
    @Async
    @Transactional
    public void saveAdLogAsync(String traceId, String clientIp, String apiKey, String username, boolean success, String failureReason, Map<String, Object> attrs) {
        if (traceId != null && !traceId.isBlank()) {
            MDC.put("traceId", traceId);
        }

        try {
            log.info("ASYNC: Saving AD log entry for user: {} [success={}, failureReason={}]", sanitizeUtf8(username), success, failureReason);

            String attrsJson = serializeAttrs(attrs, username);

            AdLog adLog = AdLog.builder()
                    .traceId(sanitizeUtf8(traceId))
                    .clientIp(sanitizeUtf8(clientIp))
                    .apiKey(sanitizeUtf8(apiKey))
                    .username(sanitizeUtf8(username))
                    .success(success)
                    .failureReason(sanitizeUtf8(failureReason))
                    .adAttributes(attrsJson)
                    .calledAt(LocalDateTime.now())
                    .build();

            adLogRepository.save(adLog);
            log.info("ASYNC: Successfully saved AD log ID: {} for user: {} [success={}]", adLog.getId(), sanitizeUtf8(username), success);
        } catch (Exception e) {
            log.error("ASYNC: Failed to save AD log entry for user {}: {}", sanitizeUtf8(username), e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("SHUTDOWN: Flushing all remaining background user cache & system user queues...");
        flushUserCacheQueue();
        flushSystemUserQueue();
        log.info("SHUTDOWN: Background queues flushed successfully.");
    }

    private String serializeAttrs(Map<String, Object> attrs, String username) {
        if (attrs == null) {
            return null;
        }
        try {
            return sanitizeUtf8(objectMapper.writeValueAsString(attrs));
        } catch (JsonProcessingException e) {
            log.warn("ASYNC: Failed to serialize AD attributes for user {}: {}", sanitizeUtf8(username), e.getMessage());
            return null;
        }
    }

    private String serializeMemberOf(Map<String, Object> attrs, String username) {
        if (attrs == null || attrs.get("memberOf") == null) {
            return null;
        }
        try {
            return sanitizeUtf8(objectMapper.writeValueAsString(attrs.get("memberOf")));
        } catch (JsonProcessingException e) {
            log.warn("ASYNC: Failed to serialize memberOf for user {}: {}", sanitizeUtf8(username), e.getMessage());
            return null;
        }
    }

    private String sanitizeUtf8(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\u0000", "").replace("\0", "");
    }
}
