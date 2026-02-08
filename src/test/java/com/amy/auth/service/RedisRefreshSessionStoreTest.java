package com.amy.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RedisRefreshSessionStoreTest {

    @Test
    void putWritesSessionAndIndexes() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper om = mock(ObjectMapper.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);

        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(sets);
        when(om.writeValueAsString(any())).thenReturn("{\"ok\":true}");

        RedisRefreshSessionStore store = new RedisRefreshSessionStore(redis, om, "rt:", "rtu:", "rtd:");

        var view = new RedisRefreshSessionStore.RefreshSessionView(
                UUID.randomUUID(), "device", UUID.randomUUID(), OffsetDateTime.now().plusDays(1)
        );
        store.put("hash", view, Duration.ofHours(1));

        verify(values).set(eq("rt:hash"), eq("{\"ok\":true}"), eq(Duration.ofHours(1)));
        verify(sets).add("rtu:" + view.userId(), "hash");
        verify(sets).add("rtd:" + view.userId() + ":" + view.deviceId(), "hash");
        verify(redis).expire("rtu:" + view.userId(), Duration.ofHours(7));
        verify(redis).expire("rtd:" + view.userId() + ":" + view.deviceId(), Duration.ofHours(7));
    }

    @Test
    void getIfPresentReadsAndParses() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper om = mock(ObjectMapper.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);

        when(redis.opsForValue()).thenReturn(values);
        when(values.get("rt:hash")).thenReturn("{\"ok\":true}");

        var view = new RedisRefreshSessionStore.RefreshSessionView(
                UUID.randomUUID(), "device", UUID.randomUUID(), OffsetDateTime.now().plusDays(1)
        );
        when(om.readValue("{\"ok\":true}", RedisRefreshSessionStore.RefreshSessionView.class)).thenReturn(view);

        RedisRefreshSessionStore store = new RedisRefreshSessionStore(redis, om, "rt:", "rtu:", "rtd:");

        assertThat(store.getIfPresent("hash")).isEqualTo(view);
    }

    @Test
    void getIfPresentReturnsNullWhenMissing() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper om = mock(ObjectMapper.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);

        when(redis.opsForValue()).thenReturn(values);
        when(values.get("rt:hash")).thenReturn(null);

        RedisRefreshSessionStore store = new RedisRefreshSessionStore(redis, om, "rt:", "rtu:", "rtd:");

        assertThat(store.getIfPresent("hash")).isNull();
    }

    @Test
    void getIfPresentWrapsExceptions() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper om = mock(ObjectMapper.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);

        when(redis.opsForValue()).thenReturn(values);
        when(values.get("rt:hash")).thenReturn("bad");
        when(om.readValue("bad", RedisRefreshSessionStore.RefreshSessionView.class)).thenThrow(new RuntimeException("boom"));

        RedisRefreshSessionStore store = new RedisRefreshSessionStore(redis, om, "rt:", "rtu:", "rtd:");

        assertThatThrownBy(() -> store.getIfPresent("hash"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis refresh session read failed");
    }

    @Test
    void putWrapsExceptions() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper om = mock(ObjectMapper.class);

        when(om.writeValueAsString(any())).thenThrow(new RuntimeException("boom"));

        RedisRefreshSessionStore store = new RedisRefreshSessionStore(redis, om, "rt:", "rtu:", "rtd:");
        var view = new RedisRefreshSessionStore.RefreshSessionView(
                UUID.randomUUID(), "device", UUID.randomUUID(), OffsetDateTime.now().plusDays(1)
        );

        assertThatThrownBy(() -> store.put("hash", view, Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis refresh session write failed");
    }

    @Test
    void deleteAndRevokeMethodsCleanKeys() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redis.opsForSet()).thenReturn(sets);

        RedisRefreshSessionStore store = new RedisRefreshSessionStore(redis, mock(ObjectMapper.class), "rt:", "rtu:", "rtd:");
        UUID userId = UUID.randomUUID();

        store.delete("hash", userId, "device");
        verify(redis).delete("rt:hash");
        verify(sets).remove("rtu:" + userId, "hash");
        verify(sets).remove("rtd:" + userId + ":device", "hash");

        when(sets.members("rtu:" + userId)).thenReturn(Set.of("h1", "h2"));
        store.revokeAllForUser(userId);
        verify(redis).delete("rt:h1");
        verify(redis).delete("rt:h2");
        verify(redis).delete("rtu:" + userId);

        when(sets.members("rtd:" + userId + ":device")).thenReturn(Set.of("h3"));
        store.revokeAllForUserDevice(userId, "device");
        verify(redis).delete("rt:h3");
        verify(redis).delete("rtd:" + userId + ":device");
    }
}
