package com.electrahub.identity.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "address")
public class Address {
    private static final Logger LOGGER = LoggerFactory.getLogger(Address.class);

    @Id
    private UUID id;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    protected Address() {}

    /**
     * Creates address for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.domain`.
     * @param id input consumed by Address.
     * @param street input consumed by Address.
     * @param city input consumed by Address.
     * @param state input consumed by Address.
     * @param postalCode input consumed by Address.
     * @param country input consumed by Address.
     */
    public Address(UUID id, String street, String city, String state, String postalCode, Country country) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering Address#Address");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering Address#Address with debug context");
        this.id = id;
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public UUID getId() { return id; }
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public Country getCountry() { return country; }
}
