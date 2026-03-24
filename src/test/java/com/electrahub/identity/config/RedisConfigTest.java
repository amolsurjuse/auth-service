package com.electrahub.identity.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfigTest.class);


    /**
     * Executes redis connection factory uses defaults for `RedisConfigTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void redisConnectionFactoryUsesDefaults() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RedisConfigTest#redisConnectionFactoryUsesDefaults");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RedisConfigTest#redisConnectionFactoryUsesDefaults with debug context");
        RedisConfig config = new RedisConfig();
        MockEnvironment env = new MockEnvironment();

        LettuceConnectionFactory factory = config.redisConnectionFactory(env);

        assertThat(factory.getStandaloneConfiguration().getHostName()).isEqualTo("redis");
        assertThat(factory.getStandaloneConfiguration().getPort()).isEqualTo(6379);
    }

    /**
     * Executes redis connection factory uses configured values for `RedisConfigTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void redisConnectionFactoryUsesConfiguredValues() {
        RedisConfig config = new RedisConfig();
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.data.redis.host", "localhost")
                .withProperty("spring.data.redis.port", "6380")
                .withProperty("spring.data.redis.password", "secret");

        LettuceConnectionFactory factory = config.redisConnectionFactory(env);

        assertThat(factory.getStandaloneConfiguration().getHostName()).isEqualTo("localhost");
        assertThat(factory.getStandaloneConfiguration().getPort()).isEqualTo(6380);
        assertThat(factory.getStandaloneConfiguration().getPassword().isPresent()).isTrue();
    }

    /**
     * Executes string redis template builds for `RedisConfigTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void stringRedisTemplateBuilds() {
        RedisConfig config = new RedisConfig();
        LettuceConnectionFactory factory = new LettuceConnectionFactory();

        StringRedisTemplate template = config.stringRedisTemplate(factory);

        assertThat(template).isNotNull();
    }
}
