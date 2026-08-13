package com.backend.features.auth.service.impl;

import com.backend.enums.common.AdUserStatus;
import com.backend.enums.common.Status;
import com.backend.exception.custom.NotFoundException;
import com.backend.exception.custom.ValidationException;
import com.backend.features.auth.dto.request.AdSystemUserUnlockRequest;
import com.backend.features.auth.dto.request.LoginRequest;
import com.backend.features.auth.dto.response.AdSystemUserResponse;
import com.backend.features.auth.dto.response.AdSystemUserUnlockResponse;
import com.backend.features.auth.dto.response.AdUserDto;
import com.backend.features.auth.dto.response.LoginResponse;
import com.backend.features.auth.mapper.AdMapper;
import com.backend.features.auth.mapper.AdSystemUserMapper;
import com.backend.features.auth.model.AdApiKey;
import com.backend.features.auth.model.AdConfig;
import com.backend.features.auth.model.AdSystemUser;
import com.backend.features.auth.model.AdUserCache;
import com.backend.features.auth.model.AdUserStatusAudit;
import com.backend.features.auth.repository.AdApiKeyRepository;
import com.backend.features.auth.repository.AdConfigRepository;
import com.backend.features.auth.repository.AdSystemUserRepository;
import com.backend.features.auth.repository.AdUserCacheRepository;
import com.backend.features.auth.repository.AdUserStatusAuditRepository;
import com.backend.features.auth.service.AdAuthAsyncService;
import com.backend.features.auth.service.AdAuthService;
import com.backend.features.auth.util.LdapAdErrorUtils;
import com.backend.shared.utils.ClientIpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdAuthServiceImpl implements AdAuthService {

    private final AdConfigRepository adConfigRepository;
    private final AdApiKeyRepository adApiKeyRepository;
    private final AdSystemUserRepository adSystemUserRepository;
    private final AdUserCacheRepository adUserCacheRepository;
    private final AdUserStatusAuditRepository adUserStatusAuditRepository;
    private final AdAuthAsyncService adAuthAsyncService;
    private final AdMapper adMapper;
    private final AdSystemUserMapper adSystemUserMapper;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Data
    @AllArgsConstructor
    private static class LockStatusResult {
        private String errorCode;
        private String message;
    }

    @Override
    public LoginResponse login(LoginRequest request, String apiKeyHeader) {
        String username = request.getUsername();
        String password = request.getPassword();
        String clientIp = resolveClientIp();
        String traceId = getTraceId();

        String apiKey = apiKeyHeader != null ? apiKeyHeader.trim() : null;

        log.info("Processing AD authentication attempt for user: {} from IP: {} [apiKey={}]", username, clientIp, apiKey);

        // 1. API Key presence validation (Header X-API-Key required)
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AD authentication rejected for user {} from IP {}: X-API-Key header is missing", username, clientIp);
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, null, username, false, "X-API-Key header is required", null);
            return LoginResponse.failure("AUTH_API_KEY_REQUIRED", "X-API-Key header is required to access AD authentication");
        }

        // 2. Validate API Key exists and is ACTIVE
        Optional<AdApiKey> apiKeyOpt = adApiKeyRepository.findByApiKeyAndIsDeletedFalse(apiKey);
        if (apiKeyOpt.isEmpty()) {
            log.warn("AD authentication rejected for user {} from IP {}: Invalid API Key [{}]", username, clientIp, apiKey);
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, false, "Invalid API Key", null);
            return LoginResponse.failure("AUTH_API_KEY_INVALID", "Invalid AD API Key");
        }

        AdApiKey apiKeyEntity = apiKeyOpt.get();
        if (apiKeyEntity.getStatus() == Status.INACTIVE) {
            log.warn("AD authentication rejected for user {} under [{}]: API Key is INACTIVE", username, apiKeyEntity.getLabel());
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, false, "API Key is inactive or locked", null);
            return LoginResponse.failure("AUTH_API_KEY_INACTIVE", "AD API Key is inactive or locked");
        }

        String appName = apiKeyEntity.getLabel();

        // 3. Validate user account mapping & lock status under this API Key
        Optional<LockStatusResult> lockResultOpt = checkUserAccountStatus(apiKey, username, appName);
        if (lockResultOpt.isPresent()) {
            LockStatusResult lockResult = lockResultOpt.get();
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, false, lockResult.getMessage(), null);
            return LoginResponse.failure(lockResult.getErrorCode(), lockResult.getMessage());
        }

        // 4. Check global AD feature toggle configuration
        AdConfig adConfig = adConfigRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> AdConfig.builder().adEnabled(true).build());

        if (!Boolean.TRUE.equals(adConfig.getAdEnabled())) {
            log.info("AD authentication is disabled in config - attempting fallback authentication from local cache for user: {}", username);
            return authenticateFromLocalCache(username, password, apiKey, clientIp, traceId, "AD authentication disabled - logged in via local cache");
        }

        // 5. Attempt live LDAP Active Directory authentication
        try {
            log.info("Connecting to LDAP Active Directory for user: {} from system: {} [IP={}]", username, appName, clientIp);
            LdapContext ctx = connectToAd(username, password);
            Map<String, Object> attrs = searchUserAttributes(ctx, username);
            AdUserDto adUser = adMapper.toAdUserDto(attrs);

            // Record previous last login before updating
            LocalDateTime previousLastLogin = getPreviousLastLogin(apiKey, username);
            adUser.setLastLoginAt(previousLastLogin);

            // Asynchronously update local user cache, system user mapping bucket & save log
            adAuthAsyncService.saveOrUpdateUserCacheAsync(traceId, username, password, adUser, attrs);
            adAuthAsyncService.saveOrUpdateSystemUserAsync(traceId, apiKey, username);
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, true, null, attrs);

            log.info("AD authentication successful for user: {} from system: {} [IP={}]", username, appName, clientIp);
            return LoginResponse.success(adUser);
        } catch (NamingException e) {
            String errorMessage = e.getMessage();
            String sanitizedError = LdapAdErrorUtils.sanitizeUtf8(errorMessage);
            String errorCode = LdapAdErrorUtils.resolveErrorCode(errorMessage);
            String friendlyMsg = LdapAdErrorUtils.resolveFriendlyErrorMessage(errorMessage);

            log.warn("AD authentication LDAP error for user {} from system {} [IP={}]: {} [errorCode={}]", username, appName, clientIp, sanitizedError, errorCode);

            String errUpper = sanitizedError != null ? sanitizedError.toUpperCase() : "";
            boolean isAccountLockOrPolicyError = errUpper.contains("775") || errUpper.contains("533") ||
                    errUpper.contains("532") || errUpper.contains("773") || errUpper.contains("701") ||
                    errUpper.contains("530") || errUpper.contains("531") || errUpper.contains("568") || errUpper.contains("5320");

            if (!isAccountLockOrPolicyError) {
                // Attempt fallback to local user cache if user credentials match and AD server connection failed
                Optional<LoginResponse> fallbackResult = tryFallbackCache(username, password, apiKey, clientIp, traceId, "AD LDAP error - fallback to local user cache");
                if (fallbackResult.isPresent()) {
                    return fallbackResult.get();
                }
            }

            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, false, friendlyMsg, null);
            return LoginResponse.failure(errorCode, friendlyMsg);
        } catch (Exception e) {
            String sanitizedMsg = LdapAdErrorUtils.sanitizeUtf8(e.getMessage());
            String errorCode = LdapAdErrorUtils.resolveErrorCode(sanitizedMsg);
            String friendlyMsg = LdapAdErrorUtils.resolveFriendlyErrorMessage(sanitizedMsg);
            log.error("Unexpected error during AD authentication for user {} from system {} [IP={}]: {} [errorCode={}]", username, appName, clientIp, sanitizedMsg, errorCode);

            // Attempt fallback to local user cache if AD server is unavailable
            Optional<LoginResponse> fallbackResult = tryFallbackCache(username, password, apiKey, clientIp, traceId, "AD server unavailable - fallback to local user cache");
            if (fallbackResult.isPresent()) {
                return fallbackResult.get();
            }

            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, false, friendlyMsg, null);
            return LoginResponse.failure(errorCode, friendlyMsg);
        }
    }

    private Optional<LockStatusResult> checkUserAccountStatus(String apiKey, String username, String appName) {
        if (apiKey == null || username == null) {
            return Optional.empty();
        }

        Optional<AdSystemUser> userOpt = adSystemUserRepository.findByApiKeyAndUsername(apiKey, username.trim());
        if (userOpt.isPresent()) {
            AdSystemUser user = userOpt.get();

            // Check if status is LOCKED
            if (user.getStatus() == AdUserStatus.LOCKED) {
                String reason = user.getLockReason() != null ? user.getLockReason() : "Account is locked for this application";
                String errorCode = (reason.contains("Inactive") || reason.contains("90 days"))
                        ? "AUTH_ACCOUNT_LOCKED_INACTIVITY"
                        : "AUTH_ACCOUNT_LOCKED_ADMIN";

                log.warn("AD authentication rejected for user {} under [{}]: Account is LOCKED ({})", username, appName, reason);
                return Optional.of(new LockStatusResult(errorCode, "Your account has been locked for application [" + appName + "]: " + reason + ". Please contact your administrator or unlock via API."));
            }

            // Check 90 days inactivity rule (last login > 90 days or created > 90 days with no login)
            LocalDateTime lastActive = user.getLastLoginAt() != null ? user.getLastLoginAt() : user.getCreatedAt();
            if (lastActive != null && lastActive.isBefore(LocalDateTime.now().minusDays(90))) {
                String lockReason = "Inactive for more than 90 days (last login: " + (user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "never") + ")";
                log.warn("AD authentication auto-locking user {} under [{}]: Inactive for > 90 days (last active: {})", username, appName, lastActive);
                user.lock(lockReason);
                adSystemUserRepository.save(user);
                recordStatusAudit(apiKey, username, "LOCKED", lockReason);
                return Optional.of(new LockStatusResult("AUTH_ACCOUNT_LOCKED_INACTIVITY", "Your account has been locked for application [" + appName + "] due to inactivity exceeding 90 days. Please unlock your account to proceed."));
            }
        }
        return Optional.empty();
    }

    private LocalDateTime getPreviousLastLogin(String apiKey, String username) {
        if (apiKey != null && username != null) {
            Optional<AdSystemUser> userOpt = adSystemUserRepository.findByApiKeyAndUsername(apiKey, username.trim());
            if (userOpt.isPresent() && userOpt.get().getLastLoginAt() != null) {
                return userOpt.get().getLastLoginAt();
            }
        }
        if (username != null) {
            return adUserCacheRepository.findByUsernameAndIsDeletedFalse(username.trim())
                    .map(AdUserCache::getLastLoginAt)
                    .orElse(null);
        }
        return null;
    }

    private LoginResponse authenticateFromLocalCache(String username, String password, String apiKey, String clientIp, String traceId, String successMessage) {
        Optional<AdUserCache> cachedOpt = adUserCacheRepository.findByUsernameAndIsDeletedFalse(username);
        if (cachedOpt.isPresent()) {
            AdUserCache cached = cachedOpt.get();
            if (password != null && passwordEncoder.matches(password, cached.getEncryptedPassword())) {
                AdUserDto adUser = buildDtoFromCache(cached);
                LocalDateTime previousLastLogin = getPreviousLastLogin(apiKey, username);
                adUser.setLastLoginAt(previousLastLogin);

                adAuthAsyncService.saveOrUpdateUserCacheAsync(traceId, username, password, adUser, null);
                adAuthAsyncService.saveOrUpdateSystemUserAsync(traceId, apiKey, username);
                adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, true, successMessage, null);
                log.info("Fallback login successful for user: {} via local cache", username);
                return LoginResponse.success(adUser, successMessage);
            } else {
                adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, false, "AD disabled - wrong password for cached user", null);
                return LoginResponse.failure("AUTH_INVALID_CREDENTIALS", "Invalid username or password");
            }
        }

        log.warn("AD authentication disabled and user {} has no local cached record", username);
        adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, false, "AD disabled - user not found in local cache", null);
        return LoginResponse.failure("AUTH_USER_NOT_CACHED", "Active Directory authentication is currently disabled and your account has not logged in before. Please contact system administrator for assistance.");
    }

    private Optional<LoginResponse> tryFallbackCache(String username, String password, String apiKey, String clientIp, String traceId, String fallbackReason) {
        Optional<AdUserCache> cachedOpt = adUserCacheRepository.findByUsernameAndIsDeletedFalse(username);
        if (cachedOpt.isPresent()) {
            AdUserCache cached = cachedOpt.get();
            if (password != null && passwordEncoder.matches(password, cached.getEncryptedPassword())) {
                AdUserDto adUser = buildDtoFromCache(cached);
                LocalDateTime previousLastLogin = getPreviousLastLogin(apiKey, username);
                adUser.setLastLoginAt(previousLastLogin);

                adAuthAsyncService.saveOrUpdateUserCacheAsync(traceId, username, password, adUser, null);
                adAuthAsyncService.saveOrUpdateSystemUserAsync(traceId, apiKey, username);
                adAuthAsyncService.saveAdLogAsync(traceId, clientIp, apiKey, username, true, "AD fallback to local cache (" + fallbackReason + ")", null);
                log.info("AD LDAP error - Fallback login successful for user: {} via local user cache", username);
                return Optional.of(LoginResponse.success(adUser, "AD service unavailable - authenticated via local cache"));
            }
        }
        return Optional.empty();
    }

    private AdUserDto buildDtoFromCache(AdUserCache cache) {
        return AdUserDto.builder()
                .samaccountName(cache.getUsername())
                .displayName(cache.getDisplayName())
                .cn(cache.getCn())
                .givenName(cache.getGivenName())
                .sn(cache.getSn())
                .mail(cache.getMail())
                .department(cache.getDepartment())
                .title(cache.getTitle())
                .telephoneNumber(cache.getTelephoneNumber())
                .mobile(cache.getMobile())
                .company(cache.getCompany())
                .distinguishedName(cache.getDistinguishedName())
                .lastLoginAt(cache.getLastLoginAt())
                .build();
    }

    private LdapContext connectToAd(String username, String password) throws NamingException {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_PRINCIPAL, username + "@adcpbank.com");
        props.put(Context.SECURITY_CREDENTIALS, password);
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, "ldap://adcpbank.com/");
        return new InitialLdapContext(props, null);
    }

    private Map<String, Object> searchUserAttributes(LdapContext ctx, String username) throws NamingException {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(null);

        NamingEnumeration<SearchResult> results = ctx.search(
                "DC=adcpbank,DC=com",
                "(sAMAccountName=" + username + ")",
                controls
        );

        if (results.hasMore()) {
            SearchResult result = results.next();
            Attributes attributes = result.getAttributes();
            NamingEnumeration<? extends Attribute> allAttrs = attributes.getAll();

            Map<String, Object> map = new LinkedHashMap<>();
            while (allAttrs.hasMore()) {
                Attribute attr = allAttrs.next();
                if (attr.size() == 1) {
                    map.put(attr.getID(), sanitize(attr.get().toString()));
                } else {
                    List<String> values = new ArrayList<>();
                    NamingEnumeration<?> vals = attr.getAll();
                    while (vals.hasMore()) {
                        values.add(sanitize(vals.next().toString()));
                    }
                    map.put(attr.getID(), values);
                }
            }
            return map;
        }
        return Map.of();
    }

    private String sanitize(String str) {
        if (str == null) return null;
        return str.replace("\n", " ").replace("\r", " ").replace("\u0000", "").replace("\0", "");
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return ClientIpUtils.getClientIp(attributes.getRequest());
        }
        return "UNKNOWN";
    }

    @Override
    @Transactional
    public AdSystemUserUnlockResponse unlockUser(AdSystemUserUnlockRequest request, String apiKeyHeader) {
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            throw new ValidationException("X-API-Key header is required to unlock user");
        }

        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required");
        }

        final String apiKey = apiKeyHeader.trim();

        // Validate API Key exists & active
        AdApiKey apiKeyEntity = adApiKeyRepository.findByApiKeyAndIsDeletedFalse(apiKey)
                .orElseThrow(() -> new ValidationException("Invalid AD API Key"));

        if (apiKeyEntity.getStatus() == Status.INACTIVE) {
            throw new ValidationException("AD API Key is inactive or locked");
        }

        log.info("External request to UNLOCK user: {} under system label: [{}]", username, apiKeyEntity.getLabel());

        AdSystemUser user = adSystemUserRepository.findByApiKeyAndUsername(apiKey, username)
                .orElseGet(() -> AdSystemUser.builder()
                        .apiKey(apiKey)
                        .username(username)
                        .status(AdUserStatus.ACTIVE)
                        .build());

        user.unlock();
        if (request.getReason() != null && !request.getReason().isBlank()) {
            user.setLockReason("Unlocked: " + request.getReason().trim());
        }

        // Also update local user cache so inactivity timer is reset across both tables
        adUserCacheRepository.findByUsernameAndIsDeletedFalse(username)
                .ifPresent(cached -> {
                    cached.setLastLoginAt(user.getLastLoginAt());
                    adUserCacheRepository.save(cached);
                });

        AdSystemUser unlockedUser = adSystemUserRepository.save(user);

        // Record audit trail in ad_user_status_audits table
        String unlockReason = request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : "Account unlocked via API request";

        recordStatusAudit(apiKey, username, "UNLOCKED", unlockReason);

        log.info("Successfully unlocked user: {} under API key [{}]", username, apiKeyEntity.getLabel());
        return AdSystemUserUnlockResponse.success(adSystemUserMapper.toResponse(unlockedUser));
    }

    @Override
    @Transactional
    public int lockInactiveUsers(int inactivityDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(inactivityDays);
        log.info("Checking for inactive users with no login since {} (threshold: {} days)", cutoffDate, inactivityDays);

        List<AdSystemUser> inactiveUsers = adSystemUserRepository.findInactiveUsers(AdUserStatus.ACTIVE, cutoffDate);

        if (inactiveUsers.isEmpty()) {
            log.info("No inactive users found to lock.");
            return 0;
        }

        for (AdSystemUser user : inactiveUsers) {
            String reason = "Inactive for more than " + inactivityDays + " days (last login: " +
                    (user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "never") + ")";
            user.lock(reason);
            log.info("Auto-locked inactive user: {} under API Key - Reason: {}", user.getUsername(), reason);
            recordStatusAudit(user.getApiKey(), user.getUsername(), "LOCKED", reason);
        }

        adSystemUserRepository.saveAll(inactiveUsers);
        log.info("Successfully auto-locked {} inactive user(s)", inactiveUsers.size());
        return inactiveUsers.size();
    }

    private void recordStatusAudit(String apiKey, String username, String action, String reason) {
        try {
            AdUserStatusAudit audit = AdUserStatusAudit.builder()
                    .apiKey(apiKey)
                    .username(username)
                    .action(action)
                    .reason(reason)
                    .build();
            adUserStatusAuditRepository.save(audit);
            log.info("Recorded user status audit: [{}] for user {} under API key (reason: {})", action, username, reason);
        } catch (Exception e) {
            log.error("Failed to save user status audit for user {}: {}", username, e.getMessage(), e);
        }
    }
}
