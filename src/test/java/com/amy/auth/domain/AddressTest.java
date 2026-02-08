package com.amy.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    @Test
    void constructorSetsFields() {
        Country country = new Country(UUID.randomUUID(), "US", "United States");
        Address address = new Address(UUID.randomUUID(), "street", "city", "state", "12345", country);

        assertThat(address.getStreet()).isEqualTo("street");
        assertThat(address.getCity()).isEqualTo("city");
        assertThat(address.getState()).isEqualTo("state");
        assertThat(address.getPostalCode()).isEqualTo("12345");
        assertThat(address.getCountry()).isEqualTo(country);
    }
}
