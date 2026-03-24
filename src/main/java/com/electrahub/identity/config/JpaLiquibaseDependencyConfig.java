package com.electrahub.identity.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true", matchIfMissing = true)
public class JpaLiquibaseDependencyConfig extends EntityManagerFactoryDependsOnPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaLiquibaseDependencyConfig.class);


    /**
     * Executes jpa liquibase dependency config for `JpaLiquibaseDependencyConfig`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    public JpaLiquibaseDependencyConfig() {
        super("liquibase");
        LOGGER.info("CODEx_ENTRY_LOG: Entering JpaLiquibaseDependencyConfig#JpaLiquibaseDependencyConfig");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering JpaLiquibaseDependencyConfig#JpaLiquibaseDependencyConfig with debug context");
    }
}
