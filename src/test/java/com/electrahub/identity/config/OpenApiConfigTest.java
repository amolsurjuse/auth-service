package com.electrahub.identity.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiConfigTest.class);


    /**
     * Executes custom open api has expected info for `OpenApiConfigTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void customOpenApiHasExpectedInfo() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering OpenApiConfigTest#customOpenApiHasExpectedInfo");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering OpenApiConfigTest#customOpenApiHasExpectedInfo with debug context");
        OpenApiConfig config = new OpenApiConfig();

        var api = config.customOpenAPI();

        assertThat(api.getInfo()).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("Auth Service API");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(api.getInfo().getDescription()).isEqualTo("OpenAPI documentation for Auth Service");
    }

    /**
     * Executes public apis group is public for `OpenApiConfigTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void publicApisGroupIsPublic() {
        OpenApiConfig config = new OpenApiConfig();

        GroupedOpenApi api = config.publicApis();

        assertThat(api.getGroup()).isEqualTo("public");
    }
}
