package com.amy.auth.domain.enums;

import java.util.Arrays;
import java.util.Optional;

public enum CountryCode {
    US("United States"),
    CA("Canada"),
    GB("United Kingdom"),
    IE("Ireland"),
    DE("Germany"),
    FR("France"),
    NL("Netherlands"),
    SE("Sweden"),
    NO("Norway"),
    DK("Denmark"),
    FI("Finland"),
    ES("Spain"),
    IT("Italy"),
    PT("Portugal"),
    CH("Switzerland"),
    AT("Austria"),
    AU("Australia"),
    NZ("New Zealand"),
    JP("Japan"),
    SG("Singapore"),
    IN("India"),
    AE("United Arab Emirates");

    private final String displayName;

    CountryCode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<CountryCode> fromCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
