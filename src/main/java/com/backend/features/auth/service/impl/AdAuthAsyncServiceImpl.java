package com.backend.features.auth.service.impl;

import com.backend.features.auth.dto.AdUserDto;
import com.backend.features.auth.model.AdLog;
import com.backend.features.auth.model.AdUserCache;
import com.backend.features.auth.repository.AdLogRepository;
import com.backend.features.auth.repository.AdUserCacheRepository;
import com.backend.features.auth.service.AdAuthAsyncService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdAuthAsyncServiceImpl implements AdAuthAsyncService {

    private final AdUserCacheRepository adUserCacheRepository;
    private final AdLogRepository adLogRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Async
    @Transactional
    public void saveOrUpdateUserCacheAsync(String username, String rawPassword, AdUserDto adUserDto, Map<String, Object> attrs) {
        log.info("ASYNC: Updating local user cache for user: {}", username);

        try {
            String encryptedPassword = rawPassword != null ? passwordEncoder.encode(rawPassword) : null;
            String attrsJson = null;

            if (attrs != null) {
                try {
                    attrsJson = objectMapper.writeValueAsString(attrs);
                } catch (JsonProcessingException e) {
                    log.warn("ASYNC: Failed to serialize AD attributes for user {}: {}", username, e.getMessage());
                }
            }

            String memberOfJson = null;
            if (adUserDto != null && adUserDto.getMemberOf() != null) {
                try {
                    memberOfJson = objectMapper.writeValueAsString(adUserDto.getMemberOf());
                } catch (JsonProcessingException e) {
                    log.warn("ASYNC: Failed to serialize memberOf for user {}: {}", username, e.getMessage());
                }
            }

            Optional<AdUserCache> existingCacheOpt = adUserCacheRepository.findByUsernameAndIsDeletedFalse(username);

            if (existingCacheOpt.isPresent()) {
                AdUserCache userCache = existingCacheOpt.get();
                if (encryptedPassword != null) {
                    userCache.setEncryptedPassword(encryptedPassword);
                }
                if (adUserDto != null) {
                    userCache.setDisplayName(adUserDto.getDisplayName());
                    userCache.setCn(adUserDto.getCn());
                    userCache.setGivenName(adUserDto.getGivenName());
                    userCache.setSn(adUserDto.getSn());
                    userCache.setMail(adUserDto.getMail());
                    userCache.setDepartment(adUserDto.getDepartment());
                    userCache.setTitle(adUserDto.getTitle());
                    userCache.setTelephoneNumber(adUserDto.getTelephoneNumber());
                    userCache.setMobile(adUserDto.getMobile());
                    userCache.setCompany(adUserDto.getCompany());
                    userCache.setDistinguishedName(adUserDto.getDistinguishedName());
                    userCache.setMemberOf(memberOfJson);
                }
                if (attrsJson != null) {
                    userCache.setAdAttributes(attrsJson);
                }
                userCache.setLastLoginAt(LocalDateTime.now());
                adUserCacheRepository.save(userCache);
                log.info("ASYNC: Successfully updated local user cache with full AD fields for user: {}", username);
            } else {
                AdUserCache newCache = AdUserCache.builder()
                        .username(username)
                        .encryptedPassword(encryptedPassword != null ? encryptedPassword : "")
                        .displayName(adUserDto != null ? adUserDto.getDisplayName() : username)
                        .cn(adUserDto != null ? adUserDto.getCn() : null)
                        .givenName(adUserDto != null ? adUserDto.getGivenName() : null)
                        .sn(adUserDto != null ? adUserDto.getSn() : null)
                        .mail(adUserDto != null ? adUserDto.getMail() : null)
                        .department(adUserDto != null ? adUserDto.getDepartment() : null)
                        .title(adUserDto != null ? adUserDto.getTitle() : null)
                        .telephoneNumber(adUserDto != null ? adUserDto.getTelephoneNumber() : null)
                        .mobile(adUserDto != null ? adUserDto.getMobile() : null)
                        .company(adUserDto != null ? adUserDto.getCompany() : null)
                        .distinguishedName(adUserDto != null ? adUserDto.getDistinguishedName() : null)
                        .memberOf(memberOfJson)
                        .adAttributes(attrsJson)
                        .lastLoginAt(LocalDateTime.now())
                        .build();
                adUserCacheRepository.save(newCache);
                log.info("ASYNC: Successfully created new local user cache with full AD fields for user: {}", username);
            }
        } catch (Exception e) {
            log.error("ASYNC: Failed to save or update local user cache for user {}: {}", username, e.getMessage(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void saveAdLogAsync(String traceId, String clientIp, String appName, String apiKey, String username, boolean success, String failureReason, Map<String, Object> attrs) {
        log.debug("ASYNC: Saving AD log entry for user: {} [success={}, traceId={}]", username, success, traceId);

        try {
            String attrsJson = null;
            if (attrs != null) {
                try {
                    attrsJson = objectMapper.writeValueAsString(attrs);
                } catch (JsonProcessingException e) {
                    log.warn("ASYNC: Failed to serialize AD attributes for log of user {}: {}", username, e.getMessage());
                }
            }

            AdLog adLog = AdLog.builder()
                    .traceId(traceId)
                    .clientIp(clientIp)
                    .appName(appName)
                    .apiKey(apiKey)
                    .username(username)
                    .success(success)
                    .failureReason(failureReason)
                    .adAttributes(attrsJson)
                    .calledAt(LocalDateTime.now())
                    .build();

            adLogRepository.save(adLog);
            log.debug("ASYNC: Successfully saved AD log ID: {} for user: {}", adLog.getId(), username);
        } catch (Exception e) {
            log.error("ASYNC: Failed to save AD log entry for user {}: {}", username, e.getMessage(), e);
        }
    }
}
