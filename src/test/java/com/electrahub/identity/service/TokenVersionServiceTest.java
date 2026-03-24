package com.electrahub.identity.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenVersionServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenVersionServiceTest.class);


    /**
     * Retrieves get version returns zero when missing for `TokenVersionServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     */
    @Test
    void getVersionReturnsZeroWhenMissing() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering TokenVersionServiceTest#getVersionReturnsZeroWhenMissing");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering TokenVersionServiceTest#getVersionReturnsZeroWhenMissing with debug context");
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        UUID userId = UUID.randomUUID();
        when(ops.get("tv:" + userId)).thenReturn(null);

        TokenVersionService service = new TokenVersionService(redis, "tv:");

        assertThat(service.getVersion(userId)).isEqualTo(0L);
    }

    /**
     * Retrieves get version parses value for `TokenVersionServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     */
    @Test
    void getVersionParsesValue() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        UUID userId = UUID.randomUUID();
        when(ops.get("tv:" + userId)).thenReturn("5");

        TokenVersionService service = new TokenVersionService(redis, "tv:");
        assertThat(service.getVersion(userId)).isEqualTo(5L);
    }

    /**
     * Executes bump version increments for `TokenVersionServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     */
    @Test
    void bumpVersionIncrements() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        UUID userId = UUID.randomUUID();
        when(ops.increment("tv:" + userId)).thenReturn(3L);

        TokenVersionService service = new TokenVersionService(redis, "tv:");
        assertThat(service.bumpVersion(userId)).isEqualTo(3L);
    }
}
