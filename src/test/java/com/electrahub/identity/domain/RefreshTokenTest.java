package com.electrahub.identity.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenTest.class);


    /**
     * Executes revoke sets flag for `RefreshTokenTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void revokeSetsFlag() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RefreshTokenTest#revokeSetsFlag");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RefreshTokenTest#revokeSetsFlag with debug context");
        UUID userId = UUID.randomUUID();
        RefreshToken token = new RefreshToken(UUID.randomUUID(), userId, "device", "hash", OffsetDateTime.now().plusDays(1), OffsetDateTime.now());

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
    }

    /**
     * Executes is expired now reflects expiry for `RefreshTokenTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void isExpiredNowReflectsExpiry() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = new RefreshToken(UUID.randomUUID(), userId, "device", "hash", OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now());
        RefreshToken valid = new RefreshToken(UUID.randomUUID(), userId, "device", "hash", OffsetDateTime.now().plusMinutes(10), OffsetDateTime.now());

        assertThat(expired.isExpiredNow()).isTrue();
        assertThat(valid.isExpiredNow()).isFalse();
    }
}
