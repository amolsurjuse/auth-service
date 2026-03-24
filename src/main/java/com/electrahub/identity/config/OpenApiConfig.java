package com.electrahub.identity.config;


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@ConditionalOnProperty(name = "openapi.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiConfig.class);


    /**
     * Executes custom open api for `OpenApiConfig`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     * @return result produced by customOpenAPI.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering OpenApiConfig#customOpenAPI");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering OpenApiConfig#customOpenAPI with debug context");
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .version("1.0.0")
                        .description("OpenAPI documentation for Auth Service"));
    }

    /**
     * Executes public apis for `OpenApiConfig`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     * @return result produced by publicApis.
     */
    @Bean
    public GroupedOpenApi publicApis() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .build();
    }
}

