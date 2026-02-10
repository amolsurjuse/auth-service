package com.electrahub.identity.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(SpringLiquibase.class)
public class JpaLiquibaseDependencyConfig extends EntityManagerFactoryDependsOnPostProcessor {

    public JpaLiquibaseDependencyConfig() {
        super("liquibase");
    }
}
