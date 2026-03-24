package com.electrahub.identity.integration;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.identity.web.dto.AddressDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class UserServiceClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceClient.class);


    private static final ParameterizedTypeReference<List<CountryView>> COUNTRY_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    /**
     * Executes value for `UserServiceClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.integration`.
     * @param userServiceBaseUrl input consumed by Value.
     * @return result produced by Value.
     */
    public UserServiceClient(@Value("${app.user-service.base-url}") String userServiceBaseUrl) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering UserServiceClient#Value");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering UserServiceClient#Value with debug context");
        this.restClient = RestClient.builder()
                .baseUrl(userServiceBaseUrl)
                .build();
    }

    /**
     * Creates register for `UserServiceClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.integration`.
     * @param request input consumed by register.
     * @return result produced by register.
     */
    public UserPrincipal register(RegisterUserRequest request) {
        return restClient.post()
                .uri("/api/v1/users")
                .body(request)
                .retrieve()
                .body(UserPrincipal.class);
    }

    /**
     * Executes authenticate for `UserServiceClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.integration`.
     * @param request input consumed by authenticate.
     * @return result produced by authenticate.
     */
    public UserPrincipal authenticate(AuthenticateUserRequest request) {
        return restClient.post()
                .uri("/api/v1/users/authenticate")
                .body(request)
                .retrieve()
                .body(UserPrincipal.class);
    }

    /**
     * Retrieves get principal for `UserServiceClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.integration`.
     * @param userId input consumed by getPrincipal.
     * @return result produced by getPrincipal.
     */
    public UserPrincipal getPrincipal(UUID userId) {
        return restClient.get()
                .uri("/api/v1/users/{userId}/principal", userId)
                .retrieve()
                .body(UserPrincipal.class);
    }

    /**
     * Retrieves list countries for `UserServiceClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.integration`.
     * @return result produced by listCountries.
     */
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
