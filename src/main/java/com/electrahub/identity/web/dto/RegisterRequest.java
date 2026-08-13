package com.electrahub.identity.web.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$",
                message = "Invalid phone number") String phoneNumber,
        AddressDto address,
        String application
) {
    public RegisterRequest(String email, String password, String firstName, String lastName,
                           String phoneNumber, AddressDto address) {
        this(email, password, firstName, lastName, phoneNumber, address, null);
    }
}
