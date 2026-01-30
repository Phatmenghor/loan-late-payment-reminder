package com.emenu.security;

import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userIdentifier) throws UsernameNotFoundException {
        User user = userRepository.findByUserIdentifierAndIsDeletedFalse(userIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userIdentifier));

        return new org.springframework.security.core.userdetails.User(
                user.getUserIdentifier(),
                user.getPassword(),
                mapRoleToAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> mapRoleToAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
}
