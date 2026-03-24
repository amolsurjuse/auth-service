package com.electrahub.identity.repository;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

class AddressRepositoryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressRepositoryTest.class);


    /**
     * Executes repository extends jpa repository for `AddressRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryExtendsJpaRepository() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering AddressRepositoryTest#repositoryExtendsJpaRepository");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering AddressRepositoryTest#repositoryExtendsJpaRepository with debug context");
        assertThat(JpaRepository.class.isAssignableFrom(AddressRepository.class)).isTrue();
    }
}
