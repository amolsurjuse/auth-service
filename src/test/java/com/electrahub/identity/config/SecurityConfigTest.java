package com.electrahub.identity.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigTest.class);


    /**
     * Executes cors configuration source uses configured origin patterns for `SecurityConfigTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void corsConfigurationSourceUsesConfiguredOriginPatterns() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering SecurityConfigTest#corsConfigurationSourceUsesConfiguredOriginPatterns");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering SecurityConfigTest#corsConfigurationSourceUsesConfiguredOriginPatterns with debug context");
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
