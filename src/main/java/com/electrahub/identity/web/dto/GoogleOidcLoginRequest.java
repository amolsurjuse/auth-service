package com.electrahub.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleOidcLoginRequest(
        @NotBlank(message = "Google ID token is required")
        String idToken,

        @Size(max = 128, message = "Nonce must be at most 128 characters")
        String nonce
) {
}
