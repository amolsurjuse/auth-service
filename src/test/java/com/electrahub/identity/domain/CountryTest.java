package com.electrahub.identity.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CountryTest {

    @Test
    void constructorSetsFields() {
        UUID id = UUID.randomUUID();
        Country country = new Country(id, "US", "United States", "+1", true);

        assertThat(country.getId()).isEqualTo(id);
        assertThat(country.getIsoCode()).isEqualTo("US");
        assertThat(country.getName()).isEqualTo("United States");
        assertThat(country.getDialCode()).isEqualTo("+1");
        assertThat(country.isEnabled()).isTrue();
    }
}
