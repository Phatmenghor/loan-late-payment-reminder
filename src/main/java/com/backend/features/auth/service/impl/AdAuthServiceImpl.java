package com.backend.features.auth.service.impl;

import com.backend.features.auth.dto.AdUserDto;
import com.backend.features.auth.dto.LoginRequest;
import com.backend.features.auth.dto.LoginResponse;
import com.backend.features.auth.mapper.AdMapper;
import com.backend.features.auth.model.AdLog;
import com.backend.features.auth.repository.AdConfigRepository;
import com.backend.features.auth.repository.AdLogRepository;
import com.backend.features.auth.service.AdAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class AdAuthServiceImpl implements AdAuthService {

    private final AdConfigRepository adConfigRepository;
    private final AdLogRepository adLogRepository;
    private final AdMapper adMapper;
    private final ObjectMapper objectMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        boolean adEnabled = adConfigRepository.findFirstByOrderByCreatedAtAsc()
                .map(c -> c.getAdEnabled())
                .orElse(true);

        if (!adEnabled) {
            log.info("AD disabled - bypassing authentication for user: {}", username);
            saveLog(username, true, null, null);
            return LoginResponse.success(null, "AD disabled - login bypassed");
        }

        try {
            log.info("AD authentication attempt for user: {}", username);
            LdapContext ctx = connectToAd(username, password);
            Map<String, Object> attrs = searchUserAttributes(ctx, username);
            AdUserDto adUser = adMapper.toAdUserDto(attrs);
            saveLog(username, true, null, attrs);
            log.info("AD authentication successful for user: {}", username);
            return LoginResponse.success(adUser);
        } catch (NamingException e) {
            log.warn("AD authentication failed for user {}: {}", username, e.getMessage());
            saveLog(username, false, e.getMessage(), null);
            return LoginResponse.failure("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during AD authentication for user {}: {}", username, e.getMessage(), e);
            saveLog(username, false, e.getMessage(), null);
            return LoginResponse.failure("Unexpected error: " + e.getMessage());
        }
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

    private void saveLog(String username, boolean success, String failureReason, Map<String, Object> attrs) {
        String attrsJson = null;
        if (attrs != null) {
            try {
                attrsJson = objectMapper.writeValueAsString(attrs);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize AD attributes for user {}: {}", username, e.getMessage());
            }
        }

        AdLog adLog = AdLog.builder()
                .username(username)
                .success(success)
                .failureReason(failureReason)
                .adAttributes(attrsJson)
                .calledAt(LocalDateTime.now())
                .build();
        adLogRepository.save(adLog);
    }
}
