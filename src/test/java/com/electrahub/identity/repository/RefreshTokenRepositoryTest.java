package com.electrahub.identity.repository;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRepositoryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenRepositoryTest.class);


    /**
     * Executes repository extends jpa repository for `RefreshTokenRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryExtendsJpaRepository() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RefreshTokenRepositoryTest#repositoryExtendsJpaRepository");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RefreshTokenRepositoryTest#repositoryExtendsJpaRepository with debug context");
        assertThat(JpaRepository.class.isAssignableFrom(RefreshTokenRepository.class)).isTrue();
    }

    /**
     * Executes repository declares custom methods for `RefreshTokenRepositoryTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     */
    @Test
    void repositoryDeclaresCustomMethods() throws Exception {
        Method findByTokenHash = RefreshTokenRepository.class.getMethod("findByTokenHash", String.class);
        Method deleteByUserId = RefreshTokenRepository.class.getMethod("deleteByUserId", UUID.class);
        Method deleteByUserIdAndDeviceId = RefreshTokenRepository.class.getMethod("deleteByUserIdAndDeviceId", UUID.class, String.class);

        assertThat(findByTokenHash.getReturnType()).isEqualTo(Optional.class);
        assertThat(deleteByUserId.getReturnType()).isEqualTo(void.class);
        assertThat(deleteByUserIdAndDeviceId.getReturnType()).isEqualTo(void.class);
    }
}
