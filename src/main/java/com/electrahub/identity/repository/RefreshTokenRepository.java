package com.electrahub.identity.repository;
import com.electrahub.identity.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    /**
     * Retrieves find by token hash for `RefreshTokenRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @param tokenHash input consumed by findByTokenHash.
     * @return result produced by findByTokenHash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    /**
     * Removes delete by user id for `RefreshTokenRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @param userId input consumed by deleteByUserId.
     */
    void deleteByUserId(UUID userId);
    /**
     * Removes delete by user id and device id for `RefreshTokenRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @param userId input consumed by deleteByUserIdAndDeviceId.
     * @param deviceId input consumed by deleteByUserIdAndDeviceId.
     */
    void deleteByUserIdAndDeviceId(UUID userId, String deviceId);
}
