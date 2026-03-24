package com.electrahub.identity.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CountryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryTest.class);


    /**
     * Executes constructor sets fields for `CountryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void constructorSetsFields() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering CountryTest#constructorSetsFields");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering CountryTest#constructorSetsFields with debug context");
        UUID id = UUID.randomUUID();
        Country country = new Country(id, "US", "United States", "+1", true);

        assertThat(country.getId()).isEqualTo(id);
        assertThat(country.getIsoCode()).isEqualTo("US");
        assertThat(country.getName()).isEqualTo("United States");
        assertThat(country.getDialCode()).isEqualTo("+1");
        assertThat(country.isEnabled()).isTrue();
    }
}
