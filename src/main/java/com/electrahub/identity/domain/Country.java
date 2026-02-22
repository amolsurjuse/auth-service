package com.electrahub.identity.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "country")
public class Country {
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

    public Country(UUID id, String isoCode, String name, String dialCode, boolean enabled) {
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
