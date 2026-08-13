package com.electrahub.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class GoogleOidcTokenVerifier {

    private final boolean enabled;
    private final List<String> allowedAudiences;
    private final JwtDecoder jwtDecoder;

    public GoogleOidcTokenVerifier(
            @Value("${app.security.oauth.google.enabled:false}") boolean enabled,
            @Value("${app.security.oauth.google.issuer:https://accounts.google.com}") String issuer,
            @Value("${app.security.oauth.google.jwk-set-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwkSetUri,
            @Value("${app.security.oauth.google.audiences:}") String audiences
    ) {
        this.enabled = enabled;
        this.allowedAudiences = Arrays.stream(audiences.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer)));
        this.jwtDecoder = decoder;
    }

    public GoogleOidcPrincipal verify(String idToken, String expectedNonce) {
        if (!enabled) {
            throw new DisabledException("Google login is not enabled");
        }
        if (allowedAudiences.isEmpty()) {
            throw new IllegalStateException("Google OAuth audience is not configured");
        }

        Jwt jwt = decode(idToken);
        if (jwt.getAudience().stream().noneMatch(allowedAudiences::contains)) {
            throw new BadCredentialsException("Invalid Google token audience");
        }
        if (expectedNonce != null && !expectedNonce.isBlank()) {
            String tokenNonce = jwt.getClaimAsString("nonce");
            if (!Objects.equals(expectedNonce, tokenNonce)) {
                throw new BadCredentialsException("Invalid Google token nonce");
            }
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        boolean emailVerified = isTrue(jwt.getClaim("email_verified"));

        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new BadCredentialsException("Google token is missing required identity claims");
        }
        if (!emailVerified) {
            throw new BadCredentialsException("Google account email is not verified");
        }

        return new GoogleOidcPrincipal(
                subject,
                email,
                emailVerified,
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("picture")
        );
    }

    private Jwt decode(String idToken) {
        try {
            return jwtDecoder.decode(idToken);
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid Google ID token", ex);
        }
    }

    private boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }
}
