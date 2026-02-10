package com.electrahub.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void revokeSetsFlag() {
        User user = new User(UUID.randomUUID(), "a@b.com", "hash", true, OffsetDateTime.now());
        RefreshToken token = new RefreshToken(UUID.randomUUID(), user, "device", "hash", OffsetDateTime.now().plusDays(1), OffsetDateTime.now());

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void isExpiredNowReflectsExpiry() {
        User user = new User(UUID.randomUUID(), "a@b.com", "hash", true, OffsetDateTime.now());
        RefreshToken expired = new RefreshToken(UUID.randomUUID(), user, "device", "hash", OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now());
        RefreshToken valid = new RefreshToken(UUID.randomUUID(), user, "device", "hash", OffsetDateTime.now().plusMinutes(10), OffsetDateTime.now());

        assertThat(expired.isExpiredNow()).isTrue();
        assertThat(valid.isExpiredNow()).isFalse();
    }
}
