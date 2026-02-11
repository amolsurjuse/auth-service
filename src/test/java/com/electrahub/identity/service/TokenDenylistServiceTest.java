package com.electrahub.identity.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenDenylistServiceTest {

    @Test
    void denyWritesToRedisWhenValid() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        TokenDenylistService service = new TokenDenylistService(redis, "deny:");
        service.deny("jti", Duration.ofMinutes(5));

        verify(ops).set("deny:jti", "1", Duration.ofMinutes(5));
    }

    @Test
    void denySkipsBlankOrInvalidTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        TokenDenylistService service = new TokenDenylistService(redis, "deny:");

        service.deny("", Duration.ofMinutes(5));
        service.deny("jti", Duration.ZERO);
        service.deny("jti", Duration.ofSeconds(-1));

        verifyNoInteractions(redis);
    }

    @Test
    void isDeniedChecksKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.hasKey("deny:jti")).thenReturn(true);

        TokenDenylistService service = new TokenDenylistService(redis, "deny:");

        assertThat(service.isDenied("jti")).isTrue();
        assertThat(service.isDenied(" ")).isFalse();
    }
}
