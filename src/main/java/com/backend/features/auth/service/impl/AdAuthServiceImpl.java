package com.backend.features.auth.service.impl;

import com.backend.enums.common.Status;
import com.backend.features.auth.dto.AdUserDto;
import com.backend.features.auth.dto.LoginRequest;
import com.backend.features.auth.dto.LoginResponse;
import com.backend.features.auth.mapper.AdMapper;
import com.backend.features.auth.model.AdApiKey;
import com.backend.features.auth.model.AdUserCache;
import com.backend.features.auth.repository.AdApiKeyRepository;
import com.backend.features.auth.repository.AdConfigRepository;
import com.backend.features.auth.repository.AdUserCacheRepository;
import com.backend.features.auth.service.AdAuthAsyncService;
import com.backend.features.auth.service.AdAuthService;
import com.backend.shared.utils.ClientIpUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
    private final AdUserCacheRepository adUserCacheRepository;
    private final AdAuthAsyncService adAuthAsyncService;
    private final AdMapper adMapper;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(LoginRequest request, String apiKeyHeader) {
        String username = request != null ? request.getUsername() : null;
        String password = request != null ? request.getPassword() : null;
        String clientIp = resolveClientIp();
        String traceId = getTraceId();

        String apiKey = apiKeyHeader != null ? apiKeyHeader.trim() : null;

        log.info("Processing AD authentication attempt for user: {} from IP: {} [X-API-Key: {}]", username, clientIp, apiKey);

        // 1. API Key presence validation (Header X-API-Key required)
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AD authentication rejected for user {} from IP {}: X-API-Key header is missing", username, clientIp);
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, "UNKNOWN", null, username, false, "X-API-Key header is required", null);
            return LoginResponse.failure("X-API-Key header is required to access AD authentication");
        }

        // 2. Lookup API key in database
        Optional<AdApiKey> apiKeyOpt = adApiKeyRepository.findByApiKeyAndIsDeletedFalse(apiKey);
        if (apiKeyOpt.isEmpty()) {
            log.warn("AD authentication rejected for user {} from IP {}: Invalid API key [{}]", username, clientIp, apiKey);
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, "UNKNOWN", apiKey, username, false, "Invalid API Key", null);
            return LoginResponse.failure("Invalid API Key");
        }

        AdApiKey keyRecord = apiKeyOpt.get();
        String appName = keyRecord.getLabel();

        // 3. API Key status check (ACTIVE vs INACTIVE / locked)
        if (keyRecord.getStatus() == Status.INACTIVE) {
            log.warn("AD authentication rejected for user {} from IP {}: API Key for application [{}] is INACTIVE (locked)", username, clientIp, appName);
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, false, "API Key is inactive or locked", null);
            return LoginResponse.failure("API Key for application [" + appName + "] is inactive or locked");
        }

        log.info("API Key validated successfully. Application: [{}] calling AD authentication for user: {} from IP: {}", appName, username, clientIp);

        // 4. Check AD enabled status in config
        boolean adEnabled = adConfigRepository.findFirstByOrderByCreatedAtAsc()
                .map(c -> c.getAdEnabled())
                .orElse(true);

        if (!adEnabled) {
            log.info("AD authentication is disabled in config - attempting fallback authentication from local cache for user: {}", username);
            return authenticateFromLocalCache(username, password, appName, apiKey, clientIp, traceId, "AD disabled - login bypassed via local cache");
        }

        // 5. Attempt LDAP Active Directory Authentication
        try {
            log.info("Connecting to LDAP Active Directory for user: {} from system: {} [IP={}]", username, appName, clientIp);
            LdapContext ctx = connectToAd(username, password);
            Map<String, Object> attrs = searchUserAttributes(ctx, username);
            AdUserDto adUser = adMapper.toAdUserDto(attrs);

            // Asynchronously update local user cache with encrypted password & save log
            adAuthAsyncService.saveOrUpdateUserCacheAsync(username, password, adUser, attrs);
            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, true, null, attrs);

            log.info("AD authentication successful for user: {} from system: {} [IP={}]", username, appName, clientIp);
            return LoginResponse.success(adUser);
        } catch (NamingException e) {
            String errorMessage = e.getMessage();
            log.warn("AD authentication LDAP error for user {} from system {} [IP={}]: {}", username, appName, clientIp, errorMessage);

            if (errorMessage != null && errorMessage.contains("data 775")) {
                adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, false, sanitize(errorMessage), null);
                return LoginResponse.failure("Your AD account is locked. Please contact IT Support to unlock your account.");
            }

            // Attempt fallback to local user cache if user credentials match
            Optional<LoginResponse> fallbackResult = tryFallbackCache(username, password, appName, apiKey, clientIp, traceId, "AD authentication failed: " + sanitize(errorMessage));
            if (fallbackResult.isPresent()) {
                return fallbackResult.get();
            }

            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, false, sanitize(errorMessage), null);

            boolean userExistsInCache = adUserCacheRepository.findByUsernameAndIsDeletedFalse(username).isPresent();
            if (!userExistsInCache) {
                return LoginResponse.failure("Active Directory authentication is currently unavailable and your account has not logged in before. Please contact system administrator for assistance.");
            }

            return LoginResponse.failure("Invalid username or password");
        } catch (Exception e) {
            log.error("Unexpected error during AD authentication for user {} from system {} [IP={}]: {}", username, appName, clientIp, e.getMessage(), e);

            // Attempt fallback to local user cache if AD server is unavailable
            Optional<LoginResponse> fallbackResult = tryFallbackCache(username, password, appName, apiKey, clientIp, traceId, "AD server unavailable: " + sanitize(e.getMessage()));
            if (fallbackResult.isPresent()) {
                return fallbackResult.get();
            }

            adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, false, sanitize(e.getMessage()), null);

            boolean userExistsInCache = adUserCacheRepository.findByUsernameAndIsDeletedFalse(username).isPresent();
            if (!userExistsInCache) {
                return LoginResponse.failure("Active Directory service is currently unavailable and your account has not logged in before. Please contact system administrator for assistance.");
            }

            return LoginResponse.failure("Authentication service unavailable");
        }
    }

    private LoginResponse authenticateFromLocalCache(String username, String password, String appName, String apiKey, String clientIp, String traceId, String successMessage) {
        Optional<AdUserCache> cachedOpt = adUserCacheRepository.findByUsernameAndIsDeletedFalse(username);
        if (cachedOpt.isPresent()) {
            AdUserCache cached = cachedOpt.get();
            if (password != null && passwordEncoder.matches(password, cached.getEncryptedPassword())) {
                AdUserDto adUser = buildDtoFromCache(cached);
                adAuthAsyncService.saveOrUpdateUserCacheAsync(username, password, adUser, null);
                adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, true, successMessage, null);
                log.info("Fallback login successful for user: {} via local cache", username);
                return LoginResponse.success(adUser, successMessage);
            } else {
                adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, false, "AD disabled - wrong password for cached user", null);
                return LoginResponse.failure("Invalid username or password");
            }
        }

        log.warn("AD authentication disabled and user {} has no local cached record", username);
        adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, false, "AD disabled - user not found in local cache", null);
        return LoginResponse.failure("Active Directory authentication is currently disabled and your account has not logged in before. Please contact system administrator for assistance.");
    }

    private Optional<LoginResponse> tryFallbackCache(String username, String password, String appName, String apiKey, String clientIp, String traceId, String fallbackReason) {
        Optional<AdUserCache> cachedOpt = adUserCacheRepository.findByUsernameAndIsDeletedFalse(username);
        if (cachedOpt.isPresent()) {
            AdUserCache cached = cachedOpt.get();
            if (password != null && passwordEncoder.matches(password, cached.getEncryptedPassword())) {
                AdUserDto adUser = buildDtoFromCache(cached);
                adAuthAsyncService.saveOrUpdateUserCacheAsync(username, password, adUser, null);
                adAuthAsyncService.saveAdLogAsync(traceId, clientIp, appName, apiKey, username, true, "AD fallback to local cache (" + fallbackReason + ")", null);
                log.info("AD LDAP error - Fallback login successful for user: {} via local user cache", username);
                return Optional.of(LoginResponse.success(adUser, "AD service unavailable - authenticated via local cache"));
            }
        }
        return Optional.empty();
    }

    private AdUserDto buildDtoFromCache(AdUserCache cache) {
        List<String> memberOf = null;
        if (cache.getMemberOf() != null && !cache.getMemberOf().isBlank()) {
            try {
                memberOf = objectMapper.readValue(cache.getMemberOf(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to deserialize memberOf JSON from cache for user {}: {}", cache.getUsername(), e.getMessage());
            }
        }

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
                .memberOf(memberOf)
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
            Map<String, Object> map = new LinkedHashMap<>();
            NamingEnumeration<? extends Attribute> allAttrs = attributes.getAll();
            while (allAttrs.hasMore()) {
                Attribute attr = allAttrs.next();
                if (attr.size() == 1) {
                    map.put(attr.getID(), attr.get());
                } else {
                    List<Object> values = new ArrayList<>();
                    NamingEnumeration<?> vals = attr.getAll();
                    while (vals.hasMore()) {
                        values.add(vals.next());
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
        return str.replace("\n", " ").replace("\r", " ");
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
}
