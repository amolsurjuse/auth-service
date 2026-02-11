package com.electrahub.identity.service.impl;


import com.electrahub.identity.service.PrincipalLookupService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PrincipalLookupService lookupService;

    public UserDetailsServiceImpl(PrincipalLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            var p = lookupService.loadByEmail(email.toLowerCase());

            var authorities = p.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();

            return org.springframework.security.core.userdetails.User
                    .withUsername(p.email())
                    .password(p.passwordHash())
                    .authorities(authorities)
                    .disabled(!p.enabled())
                    .build();
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("User not found");
        }
    }
}
