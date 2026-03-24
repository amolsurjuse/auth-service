package com.electrahub.identity.web.dto;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DtoValidationTest.class);


    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Executes login request validates email and password for `DtoValidationTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web.dto`.
     */
    @Test
    void loginRequestValidatesEmailAndPassword() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering DtoValidationTest#loginRequestValidatesEmailAndPassword");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering DtoValidationTest#loginRequestValidatesEmailAndPassword with debug context");
        LoginRequest invalid = new LoginRequest("bad", "");
        LoginRequest valid = new LoginRequest("user@example.com", "password");

        assertThat(validator.validate(invalid)).isNotEmpty();
        assertThat(validator.validate(valid)).isEmpty();
    }

    /**
     * Creates register request validates fields for `DtoValidationTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web.dto`.
     */
    @Test
    void registerRequestValidatesFields() {
        RegisterRequest invalid = new RegisterRequest(
                "bad",
                "short",
                "",
                "",
                "123",
                new AddressDto("street", "city", "state", "12345", "US")
        );

        RegisterRequest valid = new RegisterRequest(
                "user@example.com",
                "longenoughpassword",
                "First",
                "Last",
                "+12345678901",
                new AddressDto("street", "city", "state", "12345", "US")
        );

        assertThat(validator.validate(invalid)).isNotEmpty();
        assertThat(validator.validate(valid)).isEmpty();
    }

    /**
     * Creates address dto holds fields for `DtoValidationTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web.dto`.
     */
    @Test
    void addressDtoHoldsFields() {
        AddressDto dto = new AddressDto("street", "city", "state", "12345", "US");
        assertThat(dto.street()).isEqualTo("street");
        assertThat(dto.city()).isEqualTo("city");
        assertThat(dto.state()).isEqualTo("state");
        assertThat(dto.postalCode()).isEqualTo("12345");
        assertThat(dto.countryIsoCode()).isEqualTo("US");
    }
}
