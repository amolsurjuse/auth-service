package com.amy.auth.config;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void customOpenApiHasExpectedInfo() {
        OpenApiConfig config = new OpenApiConfig();

        var api = config.customOpenAPI();

        assertThat(api.getInfo()).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("Auth Service API");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(api.getInfo().getDescription()).isEqualTo("OpenAPI documentation for Auth Service");
    }

    @Test
    void publicApisGroupIsPublic() {
        OpenApiConfig config = new OpenApiConfig();

        GroupedOpenApi api = config.publicApis();

        assertThat(api.getGroup()).isEqualTo("public");
    }
}
