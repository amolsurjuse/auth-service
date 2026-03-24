package com.electrahub.identity.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressTest.class);


    /**
     * Executes constructor sets fields for `AddressTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void constructorSetsFields() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering AddressTest#constructorSetsFields");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering AddressTest#constructorSetsFields with debug context");
        Country country = new Country(UUID.randomUUID(), "US", "United States", "+1", true);
        Address address = new Address(UUID.randomUUID(), "street", "city", "state", "12345", country);

        assertThat(address.getStreet()).isEqualTo("street");
        assertThat(address.getCity()).isEqualTo("city");
        assertThat(address.getState()).isEqualTo("state");
        assertThat(address.getPostalCode()).isEqualTo("12345");
        assertThat(address.getCountry()).isEqualTo(country);
    }
}
