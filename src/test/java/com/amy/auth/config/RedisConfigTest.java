package com.amy.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    void redisConnectionFactoryUsesDefaults() {
        RedisConfig config = new RedisConfig();
        MockEnvironment env = new MockEnvironment();

        LettuceConnectionFactory factory = config.redisConnectionFactory(env);

        assertThat(factory.getStandaloneConfiguration().getHostName()).isEqualTo("redis");
        assertThat(factory.getStandaloneConfiguration().getPort()).isEqualTo(6379);
    }

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

    @Test
    void stringRedisTemplateBuilds() {
        RedisConfig config = new RedisConfig();
        LettuceConnectionFactory factory = new LettuceConnectionFactory();

        StringRedisTemplate template = config.stringRedisTemplate(factory);

        assertThat(template).isNotNull();
    }
}
