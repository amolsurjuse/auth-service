package com.amy.auth.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

class AddressRepositoryTest {

    @Test
    void repositoryExtendsJpaRepository() {
        assertThat(JpaRepository.class.isAssignableFrom(AddressRepository.class)).isTrue();
    }
}
