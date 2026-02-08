package com.amy.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void constructorSetsFields() {
        UUID id = UUID.randomUUID();
        Role role = new Role(id, "ADMIN");

        assertThat(role.getId()).isEqualTo(id);
        assertThat(role.getName()).isEqualTo("ADMIN");
    }
}
