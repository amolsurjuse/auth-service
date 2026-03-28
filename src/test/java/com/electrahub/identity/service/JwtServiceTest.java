package com.electrahub.identity.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtServiceTest.class);


    /**
     * Executes generate and parse access token for `JwtServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     */
    @Test
    void generateAndParseAccessToken() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering JwtServiceTest#generateAndParseAccessToken");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering JwtServiceTest#generateAndParseAccessToken with debug context");
        JwtService service = new JwtService(
                "01234567890123456789012345678901",
                "issuer",
                5,
                30
        );

        String token = service.generateAccessToken("user@example.com", "uid", 7L, List.of("USER"));
        JwtService.ParsedToken parsed = service.parseAndValidate(token);

        assertThat(parsed.subjectEmail()).isEqualTo("user@example.com");
        assertThat(parsed.uid()).isEqualTo("uid");
        assertThat(parsed.tv()).isEqualTo(7L);
    }

    /**
     * Executes parse rejects invalid issuer for `JwtServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     */
    @Test
    void parseRejectsInvalidIssuer() {
        JwtService good = new JwtService(
                "01234567890123456789012345678901",
                "issuer",
                5,
                30
        );
        JwtService bad = new JwtService(
                "01234567890123456789012345678901",
                "other",
                5,
                30
        );

        String token = bad.generateAccessToken("user@example.com", "uid", 1L, List.of());

        assertThatThrownBy(() -> good.parseAndValidate(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid issuer");
    }

    /**
     * Executes is not expired checks date for `JwtServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     */
    @Test
    void isNotExpiredChecksDate() {
        JwtService service = new JwtService(
                "01234567890123456789012345678901",
                "issuer",
                5,
                30
        );

        assertThat(service.isNotExpired(new Date(System.currentTimeMillis() + 1000))).isTrue();
        assertThat(service.isNotExpired(new Date(System.currentTimeMillis() - 1000))).isFalse();
    }

    /**
     * Creates generate access token uses admin ttl for system admin role for `JwtServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     */
    @Test
    void generateAccessTokenUsesAdminTtlForSystemAdminRole() {
        JwtService service = new JwtService(
                "01234567890123456789012345678901",
                "issuer",
                5,
                30
        );

        Instant beforeIssue = Instant.now();
        String token = service.generateAccessToken("admin@example.com", "uid", 11L, List.of("SYSTEM_ADMIN"));
        JwtService.ParsedToken parsed = service.parseAndValidate(token);

        long ttlMinutes = Duration.between(beforeIssue, parsed.exp().toInstant()).toMinutes();
        assertThat(ttlMinutes).isBetween(29L, 30L);
        assertThat(parsed.roles()).contains("SYSTEM_ADMIN");
    }
}
