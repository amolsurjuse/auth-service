package com.amy.auth.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest {

    @Test
    void repositoryExtendsJpaRepository() {
        assertThat(JpaRepository.class.isAssignableFrom(UserRepository.class)).isTrue();
    }

    @Test
    void repositoryDeclaresCustomMethods() throws Exception {
        Method findByEmail = UserRepository.class.getMethod("findByEmail", String.class);
        Method existsByEmail = UserRepository.class.getMethod("existsByEmail", String.class);

        assertThat(findByEmail.getReturnType()).isEqualTo(Optional.class);
        assertThat(existsByEmail.getReturnType()).isEqualTo(boolean.class);
    }
}
