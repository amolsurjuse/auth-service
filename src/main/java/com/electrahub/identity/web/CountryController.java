package com.electrahub.identity.web;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.identity.integration.UserServiceClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryController.class);


    private final UserServiceClient userServiceClient;

    /**
     * Executes country controller for `CountryController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web`.
     * @param userServiceClient input consumed by CountryController.
     */
    public CountryController(UserServiceClient userServiceClient) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering CountryController#CountryController");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering CountryController#CountryController with debug context");
        this.userServiceClient = userServiceClient;
    }

    public record CountryView(String code, String name, String dialCode) {}

    /**
     * Retrieves list for `CountryController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web`.
     * @return result produced by list.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CountryView> list() {
        return userServiceClient.listCountries().stream()
                .map(c -> new CountryView(c.code(), c.name(), c.dialCode()))
                .toList();
    }
}
