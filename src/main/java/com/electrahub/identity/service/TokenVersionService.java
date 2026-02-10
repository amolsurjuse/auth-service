package com.electrahub.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TokenVersionService {

    private final StringRedisTemplate redis;
    private final String prefix;

    public TokenVersionService(StringRedisTemplate redis,
                               @Value("${app.redis.token-version-prefix}") String prefix) {
        this.redis = redis;
        this.prefix = prefix;
    }

    public long getVersion(UUID userId) {
        String v = redis.opsForValue().get(prefix + userId);
        return (v == null) ? 0L : Long.parseLong(v);
    }

    public long bumpVersion(UUID userId) {
        Long v = redis.opsForValue().increment(prefix + userId);
        return (v == null) ? 0L : v;
    }
}

