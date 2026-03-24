package com.electrahub.identity.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfig.class);


    /**
     * Executes redis connection factory for `RedisConfig`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     * @param env input consumed by redisConnectionFactory.
     * @return result produced by redisConnectionFactory.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(Environment env) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RedisConfig#redisConnectionFactory");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RedisConfig#redisConnectionFactory with debug context");
        String host = env.getProperty("spring.data.redis.host", "redis");
        int port = Integer.parseInt(env.getProperty("spring.data.redis.port", "6379"));
        String password = env.getProperty("spring.data.redis.password");

        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            cfg.setPassword(password);
        }
        return new LettuceConnectionFactory(cfg);
    }

    /**
     * Executes string redis template for `RedisConfig`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     * @param redisConnectionFactory input consumed by stringRedisTemplate.
     * @return result produced by stringRedisTemplate.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
