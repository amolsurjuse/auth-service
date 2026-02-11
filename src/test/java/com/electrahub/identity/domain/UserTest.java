package com.electrahub.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void constructorNormalizesEmailAndSetsDefaults() {
        OffsetDateTime now = OffsetDateTime.now();
        User user = new User(UUID.randomUUID(), "User@Example.com", "hash", true, now);

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void addRoleAddsRole() {
        User user = new User(UUID.randomUUID(), "a@b.com", "hash", true, OffsetDateTime.now());
        Role role = new Role(UUID.randomUUID(), "USER");

        user.addRole(role);

        assertThat(user.getRoles()).contains(role);
    }

    @Test
    void preUpdateUpdatesTimestamp() {
        OffsetDateTime now = OffsetDateTime.now().minusDays(1);
        User user = new User(UUID.randomUUID(), "a@b.com", "hash", true, now);

        user.preUpdate();

        OffsetDateTime updatedAt = (OffsetDateTime) getField(user, "updatedAt");
        assertThat(updatedAt).isAfter(now);
    }

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
