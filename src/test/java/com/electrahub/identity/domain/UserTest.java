package com.electrahub.identity.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserTest.class);


    /**
     * Executes constructor normalizes email and sets defaults for `UserTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void constructorNormalizesEmailAndSetsDefaults() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering UserTest#constructorNormalizesEmailAndSetsDefaults");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering UserTest#constructorNormalizesEmailAndSetsDefaults with debug context");
        OffsetDateTime now = OffsetDateTime.now();
        User user = new User(UUID.randomUUID(), "User@Example.com", "hash", true, now);

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getRoles()).isEmpty();
    }

    /**
     * Creates add role adds role for `UserTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void addRoleAddsRole() {
        User user = new User(UUID.randomUUID(), "a@b.com", "hash", true, OffsetDateTime.now());
        Role role = new Role(UUID.randomUUID(), "USER");

        user.addRole(role);

        assertThat(user.getRoles()).contains(role);
    }

    /**
     * Executes pre update updates timestamp for `UserTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void preUpdateUpdatesTimestamp() {
        OffsetDateTime now = OffsetDateTime.now().minusDays(1);
        User user = new User(UUID.randomUUID(), "a@b.com", "hash", true, now);

        user.preUpdate();

        OffsetDateTime updatedAt = (OffsetDateTime) getField(user, "updatedAt");
        assertThat(updatedAt).isAfter(now);
    }

    /**
     * Updates setters set profile fields for `UserTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     */
    @Test
    void settersSetProfileFields() {
        User user = new User(UUID.randomUUID(), "a@b.com", "hash", true, OffsetDateTime.now());
        Address address = new Address(UUID.randomUUID(), "street", "city", "state", "12345", null);

        user.setFirstName("First");
        user.setLastName("Last");
        user.setPhoneNumber("+12345678901");
        user.setAddress(address);

        assertThat(user.getFirstName()).isEqualTo("First");
        assertThat(user.getLastName()).isEqualTo("Last");
        assertThat(user.getPhoneNumber()).isEqualTo("+12345678901");
        assertThat(user.getAddress()).isEqualTo(address);
    }

    /**
     * Retrieves get field for `UserTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     * @param user input consumed by getField.
     * @param name input consumed by getField.
     * @return result produced by getField.
     */
    private static Object getField(User user, String name) {
        try {
            var field = User.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
