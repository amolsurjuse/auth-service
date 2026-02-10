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

    protected Country() {}

    public Country(UUID id, String isoCode, String name) {
        this.id = id;
        this.isoCode = isoCode;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getIsoCode() { return isoCode; }
    public String getName() { return name; }
}
