package com.electrahub.identity.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class TokenVersionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenVersionService.class);


    private final StringRedisTemplate redis;
    private final String prefix;
    private final Duration versionTtl;

    TokenVersionService(StringRedisTemplate redis, String prefix) {
        this(redis, prefix, 35L);
    }

    public TokenVersionService(StringRedisTemplate redis,
                               /**
                                * Executes value for `TokenVersionService`.
                                *
                                * <p>Detailed behavior: follows the current implementation path and
                                * enforces component-specific rules in `com.electrahub.identity.service`.
                                * @param prefix input consumed by Value.
                                * @return result produced by Value.
                                */
                               @Value("${app.redis.token-version-prefix}") String prefix,
                               @Value("${app.redis.token-version-ttl-days:35}") long versionTtlDays) {
                                   LOGGER.info(" Entering TokenVersionService#Value");
                                   LOGGER.debug(" Entering TokenVersionService#Value with debug context");
        this.redis = redis;
        this.prefix = prefix;
        this.versionTtl = Duration.ofDays(Math.max(1L, versionTtlDays));
    }

    /**
     * Retrieves get version for `TokenVersionService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param userId input consumed by getVersion.
     * @return result produced by getVersion.
     */
    public long getVersion(UUID userId) {
        String v = redis.opsForValue().get(prefix + userId);
        return (v == null) ? 0L : Long.parseLong(v);
    }

    /**
     * Executes bump version for `TokenVersionService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param userId input consumed by bumpVersion.
     * @return result produced by bumpVersion.
     */
    public long bumpVersion(UUID userId) {
        String key = prefix + userId;
        Long v = redis.opsForValue().increment(key);
        redis.expire(key, versionTtl);
        return (v == null) ? 0L : v;
    }
}

