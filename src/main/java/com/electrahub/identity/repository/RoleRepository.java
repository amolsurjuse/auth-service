package com.electrahub.identity.repository;

import com.electrahub.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    /**
     * Retrieves find by name for `RoleRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @param name input consumed by findByName.
     * @return result produced by findByName.
     */
    Optional<Role> findByName(String name);
}

