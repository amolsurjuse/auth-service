package com.electrahub.identity.integration;

import com.electrahub.identity.web.dto.AddressDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class UserServiceClient {

    private static final ParameterizedTypeReference<List<CountryView>> COUNTRY_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public UserServiceClient(@Value("${app.user-service.base-url}") String userServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(userServiceBaseUrl)
                .build();
    }

    public UserPrincipal register(RegisterUserRequest request) {
        return restClient.post()
                .uri("/api/v1/users")
                .body(request)
                .retrieve()
                .body(UserPrincipal.class);
    }

    public UserPrincipal authenticate(AuthenticateUserRequest request) {
        return restClient.post()
                .uri("/api/v1/users/authenticate")
                .body(request)
                .retrieve()
                .body(UserPrincipal.class);
    }

    public UserPrincipal getPrincipal(UUID userId) {
        return restClient.get()
                .uri("/api/v1/users/{userId}/principal", userId)
                .retrieve()
                .body(UserPrincipal.class);
    }

    public List<CountryView> listCountries() {
        List<CountryView> response = restClient.get()
                .uri("/api/v1/countries")
                .retrieve()
                .body(COUNTRY_LIST_TYPE);
        return response == null ? List.of() : response;
    }

    public record RegisterUserRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            AddressDto address
    ) {
    }

    public record AuthenticateUserRequest(String email, String password) {
    }

    public record UserPrincipal(UUID userId, String email, boolean enabled, List<String> roles) {
    }

    public record CountryView(String code, String name, String dialCode) {
    }
}
