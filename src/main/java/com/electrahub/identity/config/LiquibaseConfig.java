package com.electrahub.identity.config;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LiquibaseConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true", matchIfMissing = true)
    SpringLiquibase liquibase(DataSource dataSource, Environment environment) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(environment.getProperty(
                "spring.liquibase.change-log",
                "classpath:/db/changelog/db.changelog-master.yaml"
        ));

        String contexts = environment.getProperty("spring.liquibase.contexts");
        if (contexts != null && !contexts.isBlank()) {
            liquibase.setContexts(contexts);
        }

        String defaultSchema = environment.getProperty("spring.liquibase.default-schema");
        if (defaultSchema != null && !defaultSchema.isBlank()) {
            liquibase.setDefaultSchema(defaultSchema);
        }

        String liquibaseSchema = environment.getProperty("spring.liquibase.liquibase-schema");
        if (liquibaseSchema != null && !liquibaseSchema.isBlank()) {
            liquibase.setLiquibaseSchema(liquibaseSchema);
        }

        String changelogTable = environment.getProperty("spring.liquibase.database-change-log-table");
        if (changelogTable != null && !changelogTable.isBlank()) {
            liquibase.setDatabaseChangeLogTable(changelogTable);
        }

        String lockTable = environment.getProperty("spring.liquibase.database-change-log-lock-table");
        if (lockTable != null && !lockTable.isBlank()) {
            liquibase.setDatabaseChangeLogLockTable(lockTable);
        }

        String dropFirst = environment.getProperty("spring.liquibase.drop-first");
        if (dropFirst != null && !dropFirst.isBlank()) {
            liquibase.setDropFirst(Boolean.parseBoolean(dropFirst));
        }

        liquibase.setShouldRun(true);
        return liquibase;
    }
}
