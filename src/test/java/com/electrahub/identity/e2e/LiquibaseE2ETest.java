package com.electrahub.identity.e2e;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.identity.integration.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LiquibaseE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseE2ETest.class);


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private UserServiceClient userServiceClient;

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        UserServiceClient userServiceClient() {
            return org.mockito.Mockito.mock(UserServiceClient.class);
        }
    }

    @Test
    void countriesApiReturnsSeededList() throws Exception {
        when(userServiceClient.listCountries()).thenReturn(java.util.List.of(
                new UserServiceClient.CountryView("US", "United States", "+1"),
                new UserServiceClient.CountryView("IN", "India", "+91")
        ));

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
        assertThat(body[0].dialCode()).startsWith("+");
    }

    private record CountryView(String code, String name, String dialCode) {}
}
