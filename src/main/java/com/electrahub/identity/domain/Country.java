package com.electrahub.identity.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "country")
public class Country {
    private static final Logger LOGGER = LoggerFactory.getLogger(Country.class);

    @Id
    private UUID id;

    @Column(name = "iso_code", nullable = false, unique = true, length = 8)
    private String isoCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "dial_code", nullable = false, length = 8)
    private String dialCode;

    @Column(nullable = false)
    private boolean enabled = true;

    protected Country() {}

    /**
     * Executes country for `Country`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     * @param id input consumed by Country.
     * @param isoCode input consumed by Country.
     * @param name input consumed by Country.
     * @param dialCode input consumed by Country.
     * @param enabled input consumed by Country.
     */
    public Country(UUID id, String isoCode, String name, String dialCode, boolean enabled) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering Country#Country");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering Country#Country with debug context");
        this.id = id;
        this.isoCode = isoCode;
        this.name = name;
        this.dialCode = dialCode;
        this.enabled = enabled;
    }

    public UUID getId() { return id; }
    public String getIsoCode() { return isoCode; }
    public String getName() { return name; }
    public String getDialCode() { return dialCode; }
    public boolean isEnabled() { return enabled; }
}
