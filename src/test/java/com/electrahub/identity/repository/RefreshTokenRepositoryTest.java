package com.electrahub.identity.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRepositoryTest {

    @Test
    void repositoryExtendsJpaRepository() {
        assertThat(JpaRepository.class.isAssignableFrom(RefreshTokenRepository.class)).isTrue();
    }

    @Test
    void repositoryDeclaresCustomMethods() throws Exception {
        Method findByTokenHash = RefreshTokenRepository.class.getMethod("findByTokenHash", String.class);
        Method deleteByUserId = RefreshTokenRepository.class.getMethod("deleteByUser_Id", UUID.class);
        Method deleteByUserIdAndDeviceId = RefreshTokenRepository.class.getMethod("deleteByUser_IdAndDeviceId", UUID.class, String.class);

        assertThat(findByTokenHash.getReturnType()).isEqualTo(Optional.class);
        assertThat(deleteByUserId.getReturnType()).isEqualTo(void.class);
        assertThat(deleteByUserIdAndDeviceId.getReturnType()).isEqualTo(void.class);
    }
}
