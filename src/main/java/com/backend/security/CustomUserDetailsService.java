package com.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    @Value("${app.auth.username:admin}")
    private String defaultUsername;

    @Value("${app.auth.password:admin123}")
    private String defaultPassword;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (defaultUsername.equals(username)) {
            return User.builder()
                    .username(username)
                    .password(defaultPassword)
                    .authorities(getDefaultAuthorities())
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();
        }

        log.warn("User not found: {}", username);
        throw new UsernameNotFoundException("User not found: " + username);
    }

    private Set<GrantedAuthority> getDefaultAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorities;
    }
}