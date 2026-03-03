package com.electrahub.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void corsConfigurationSourceUsesConfiguredOriginPatterns() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setAllowedOriginPatterns(List.of("http://localhost:4200", "https://*.electrahub.com"));
        SecurityConfig config = new SecurityConfig(corsProperties);

        CorsConfigurationSource source = config.corsConfigurationSource();
        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
        UrlBasedCorsConfigurationSource typed = (UrlBasedCorsConfigurationSource) source;
        var cors = typed.getCorsConfigurations().get("/**");
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns())
                .containsExactly("http://localhost:4200", "https://*.electrahub.com");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}
