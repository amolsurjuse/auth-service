package com.amy.auth.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.liquibase.enabled=false"
)
@ActiveProfiles("test")
class LiquibaseE2ETest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @TestConfiguration
    static class LiquibaseTestConfig {
        @Bean
        @Primary
        SpringLiquibase springLiquibase(DataSource dataSource) {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
            liquibase.setShouldRun(true);
            return liquibase;
        }
    }

    @Test
    void liquibaseCreatesTablesAndSeedsCountries() {
        Integer changelogTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'DATABASECHANGELOG' and table_schema = 'PUBLIC'",
                Integer.class
        );
        assertThat(changelogTableCount).isNotNull();
        assertThat(changelogTableCount).isGreaterThan(0);

        Integer changelogCount = jdbcTemplate.queryForObject(
                "select count(*) from DATABASECHANGELOG",
                Integer.class
        );
        assertThat(changelogCount).isNotNull();
        assertThat(changelogCount).isGreaterThan(0);

        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'COUNTRY' and table_schema = 'PUBLIC'",
                Integer.class
        );
        assertThat(tableCount).isNotNull();
        assertThat(tableCount).isGreaterThan(0);

        Integer countryCount = jdbcTemplate.queryForObject(
                "select count(*) from country",
                Integer.class
        );
        assertThat(countryCount).isNotNull();
        assertThat(countryCount).isGreaterThan(0);
    }

    @Test
    void countriesApiReturnsSeededList() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/api/countries"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        CountryView[] body = mapper.readValue(response.body(), CountryView[].class);
        assertThat(body.length).isGreaterThan(0);
        assertThat(body[0].code()).isNotBlank();
        assertThat(body[0].name()).isNotBlank();
    }

    private record CountryView(String code, String name) {}
}
