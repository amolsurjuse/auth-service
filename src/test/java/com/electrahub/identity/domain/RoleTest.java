package com.electrahub.identity.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleTest.class);


    /**
     * Executes constructor sets fields for `RoleTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void constructorSetsFields() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RoleTest#constructorSetsFields");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RoleTest#constructorSetsFields with debug context");
        UUID id = UUID.randomUUID();
        Role role = new Role(id, "ADMIN");

        assertThat(role.getId()).isEqualTo(id);
        assertThat(role.getName()).isEqualTo("ADMIN");
    }
}
