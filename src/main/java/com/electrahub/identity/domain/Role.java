package com.electrahub.identity.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
    private static final Logger LOGGER = LoggerFactory.getLogger(Role.class);

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    protected Role() {}

    /**
     * Executes role for `Role`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     * @param id input consumed by Role.
     * @param name input consumed by Role.
     */
    public Role(UUID id, String name) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering Role#Role");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering Role#Role with debug context");
        this.id = id;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
}
