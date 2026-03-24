package com.electrahub.identity.repository;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepositoryTest.class);


    /**
     * Executes repository extends jpa repository for `UserRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryExtendsJpaRepository() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering UserRepositoryTest#repositoryExtendsJpaRepository");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering UserRepositoryTest#repositoryExtendsJpaRepository with debug context");
        assertThat(JpaRepository.class.isAssignableFrom(UserRepository.class)).isTrue();
    }

    /**
     * Executes repository declares custom methods for `UserRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryDeclaresCustomMethods() throws Exception {
        Method findByEmail = UserRepository.class.getMethod("findByEmail", String.class);
        Method existsByEmail = UserRepository.class.getMethod("existsByEmail", String.class);

        assertThat(findByEmail.getReturnType()).isEqualTo(Optional.class);
        assertThat(existsByEmail.getReturnType()).isEqualTo(boolean.class);
    }
}
