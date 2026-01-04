package com.amy.auth.repository;
import com.amy.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUser_Id(UUID userId);
    void deleteByUser_IdAndDeviceId(UUID userId, String deviceId);
}

