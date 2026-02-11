package com.electrahub.identity.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CountryRepositoryTest {

    @Test
    void repositoryExtendsJpaRepository() {
        assertThat(JpaRepository.class.isAssignableFrom(CountryRepository.class)).isTrue();
    }

    @Test
    void repositoryDeclaresFindByIsoCode() throws Exception {
        Method findByIsoCode = CountryRepository.class.getMethod("findByIsoCode", String.class);
        assertThat(findByIsoCode.getReturnType()).isEqualTo(Optional.class);
    }
}
