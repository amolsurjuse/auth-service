package com.electrahub.identity.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfigTest.class);


    /**
     * Executes cache manager builds with user details cache for `CacheConfigTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void cacheManagerBuildsWithUserDetailsCache() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering CacheConfigTest#cacheManagerBuildsWithUserDetailsCache");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering CacheConfigTest#cacheManagerBuildsWithUserDetailsCache with debug context");
        CacheConfig config = new CacheConfig();
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();

        CacheManager manager = config.cacheManager(connectionFactory, 60L);

        assertThat(manager).isNotNull();
        assertThat(manager.getCache("userDetailsByEmail")).isNotNull();
    }
}
