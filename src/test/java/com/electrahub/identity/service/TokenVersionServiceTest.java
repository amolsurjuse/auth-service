package com.electrahub.identity.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenVersionServiceTest {

    @Test
    void getVersionReturnsZeroWhenMissing() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        UUID userId = UUID.randomUUID();
        when(ops.get("tv:" + userId)).thenReturn(null);

        TokenVersionService service = new TokenVersionService(redis, "tv:");

        assertThat(service.getVersion(userId)).isEqualTo(0L);
    }

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
