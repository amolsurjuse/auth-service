package com.electrahub.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SecurityConfigTest {

    @Test
    void authenticationProviderAndEncoderPresent() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder passwordEncoder = config.passwordEncoder();
        AuthenticationProvider authenticationProvider = config.authenticationProvider(
                username -> null,
                passwordEncoder
        );

        assertThat(authenticationProvider).isInstanceOf(DaoAuthenticationProvider.class);
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder.encode("pw")).isNotBlank();
    }

    @Test
    void authenticationManagerDelegatesToConfiguration() throws Exception {
        SecurityConfig config = new SecurityConfig();
        AuthenticationConfiguration configuration = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(configuration.getAuthenticationManager()).thenReturn(manager);

        assertThat(config.authenticationManager(configuration)).isSameAs(manager);
    }
}
