package com.electrahub.identity.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleRepositoryTest {

    @Test
    void repositoryExtendsJpaRepository() {
        assertThat(JpaRepository.class.isAssignableFrom(RoleRepository.class)).isTrue();
    }

    @Test
    void repositoryDeclaresFindByName() throws Exception {
        Method findByName = RoleRepository.class.getMethod("findByName", String.class);
        assertThat(findByName.getReturnType()).isEqualTo(Optional.class);
    }
}
