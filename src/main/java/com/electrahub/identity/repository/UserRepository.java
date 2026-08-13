package com.electrahub.identity.repository;

import com.electrahub.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Retrieves find by email for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @param email input consumed by findByEmail.
     * @return result produced by findByEmail.
     */
    Optional<User> findByEmail(String email);
    /**
     * Executes exists by email for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @param email input consumed by existsByEmail.
     * @return result produced by existsByEmail.
     */
    boolean existsByEmail(String email);
}
