package com.electrahub.identity.repository;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CountryRepositoryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryRepositoryTest.class);


    /**
     * Executes repository extends jpa repository for `CountryRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryExtendsJpaRepository() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering CountryRepositoryTest#repositoryExtendsJpaRepository");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering CountryRepositoryTest#repositoryExtendsJpaRepository with debug context");
        assertThat(JpaRepository.class.isAssignableFrom(CountryRepository.class)).isTrue();
    }

    /**
     * Executes repository declares find by iso code for `CountryRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryDeclaresFindByIsoCode() throws Exception {
        Method findByIsoCode = CountryRepository.class.getMethod("findByIsoCode", String.class);
        assertThat(findByIsoCode.getReturnType()).isEqualTo(Optional.class);
    }
}
