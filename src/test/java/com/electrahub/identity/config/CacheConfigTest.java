package com.electrahub.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    void cacheManagerBuildsWithUserDetailsCache() {
        CacheConfig config = new CacheConfig();
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();

        CacheManager manager = config.cacheManager(connectionFactory, 60L);

        assertThat(manager).isNotNull();
        assertThat(manager.getCache("userDetailsByEmail")).isNotNull();
    }
}
