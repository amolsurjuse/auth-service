package com.electrahub.identity.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void generateAndParseAccessToken() {
        JwtService service = new JwtService(
                "01234567890123456789012345678901",
                "issuer",
                5
        );

        String token = service.generateAccessToken("user@example.com", "uid", 7L, List.of("USER"));
        JwtService.ParsedToken parsed = service.parseAndValidate(token);

        assertThat(parsed.subjectEmail()).isEqualTo("user@example.com");
        assertThat(parsed.uid()).isEqualTo("uid");
        assertThat(parsed.tv()).isEqualTo(7L);
    }

    @Test
    void parseRejectsInvalidIssuer() {
        JwtService good = new JwtService(
                "01234567890123456789012345678901",
                "issuer",
                5
        );
        JwtService bad = new JwtService(
                "01234567890123456789012345678901",
                "other",
                5
        );

        String token = bad.generateAccessToken("user@example.com", "uid", 1L, List.of());

        assertThatThrownBy(() -> good.parseAndValidate(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid issuer");
    }

    @Test
    void isNotExpiredChecksDate() {
        JwtService service = new JwtService(
                "01234567890123456789012345678901",
                "issuer",
                5
        );

        assertThat(service.isNotExpired(new Date(System.currentTimeMillis() + 1000))).isTrue();
        assertThat(service.isNotExpired(new Date(System.currentTimeMillis() - 1000))).isFalse();
    }
}
