package com.electrahub.identity.service;

public record FacebookOAuthPrincipal(
        String subject,
        String email,
        boolean emailVerified,
        String givenName,
        String familyName,
        String pictureUrl
) {
}
