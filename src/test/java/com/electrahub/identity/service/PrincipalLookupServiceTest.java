package com.electrahub.identity.service;

import com.electrahub.identity.domain.Role;
import com.electrahub.identity.domain.User;
import com.electrahub.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PrincipalLookupServiceTest {

    @Test
    void loadByEmailReturnsCachedPrincipal() {
        UserRepository repo = mock(UserRepository.class);
        User user = new User(UUID.randomUUID(), "User@Example.com", "hash", true, OffsetDateTime.now());
        user.addRole(new Role(UUID.randomUUID(), "USER"));
        when(repo.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        PrincipalLookupService service = new PrincipalLookupService(repo);
        var p = service.loadByEmail("User@Example.com");

        assertThat(p.email()).isEqualTo("user@example.com");
        assertThat(p.passwordHash()).isEqualTo("hash");
        assertThat(p.enabled()).isTrue();
        assertThat(p.roles()).containsExactly("USER");
    }

    @Test
    void loadByEmailThrowsWhenMissing() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        PrincipalLookupService service = new PrincipalLookupService(repo);

        assertThatThrownBy(() -> service.loadByEmail("missing@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
