package com.electrahub.identity.repository;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleRepositoryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleRepositoryTest.class);


    /**
     * Executes repository extends jpa repository for `RoleRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryExtendsJpaRepository() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RoleRepositoryTest#repositoryExtendsJpaRepository");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RoleRepositoryTest#repositoryExtendsJpaRepository with debug context");
        assertThat(JpaRepository.class.isAssignableFrom(RoleRepository.class)).isTrue();
    }

    /**
     * Executes repository declares find by name for `RoleRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryDeclaresFindByName() throws Exception {
        Method findByName = RoleRepository.class.getMethod("findByName", String.class);
        assertThat(findByName.getReturnType()).isEqualTo(Optional.class);
    }
}
