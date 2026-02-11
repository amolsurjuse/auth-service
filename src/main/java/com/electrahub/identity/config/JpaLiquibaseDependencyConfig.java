package com.electrahub.identity.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true", matchIfMissing = true)
public class JpaLiquibaseDependencyConfig extends EntityManagerFactoryDependsOnPostProcessor {

    public JpaLiquibaseDependencyConfig() {
        super("liquibase");
    }
}
