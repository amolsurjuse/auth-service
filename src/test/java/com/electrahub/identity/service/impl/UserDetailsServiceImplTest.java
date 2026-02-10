package com.electrahub.identity.service.impl;

import com.electrahub.identity.service.PrincipalLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserDetailsServiceImplTest {

    @Test
    void loadUserByUsernameBuildsAuthorities() {
        PrincipalLookupService lookup = mock(PrincipalLookupService.class);
        when(lookup.loadByEmail("user@example.com")).thenReturn(
                new PrincipalLookupService.CachedPrincipal("user@example.com", "hash", true, List.of("USER", "ADMIN"))
        );

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(lookup);
        UserDetails details = service.loadUserByUsername("User@Example.com");

        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).hasSize(2);
    }

    @Test
    void loadUserByUsernameWrapsMissing() {
        PrincipalLookupService lookup = mock(PrincipalLookupService.class);
        when(lookup.loadByEmail("missing@example.com")).thenThrow(new IllegalArgumentException("User not found"));

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(lookup);

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
