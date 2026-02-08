package com.amy.auth.web.dto;

public record AddressDto(String street,
                         String city,
                         String state,
                         String postalCode,
                         String countryIsoCode)
{}
