package com.electrahub.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FacebookOAuthTokenVerifier {
    private final boolean enabled;
    private final String graphBaseUrl;
    private final RestClient restClient;

    public FacebookOAuthTokenVerifier(
            @Value("${app.security.oauth.facebook.enabled:false}") boolean enabled,
            @Value("${app.security.oauth.facebook.graph-base-url:https://graph.facebook.com}") String graphBaseUrl
    ) {
        this.enabled = enabled;
        this.graphBaseUrl = graphBaseUrl;
        this.restClient = RestClient.create();
    }

    public FacebookOAuthPrincipal verify(String accessToken) {
        if (!enabled) {
            throw new DisabledException("Facebook login is not enabled");
        }
        try {
            JsonNode profile = restClient.get()
                    .uri(graphBaseUrl + "/me?fields=id,email,first_name,last_name,picture.type(large)&access_token={accessToken}", accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (profile == null) {
                throw new BadCredentialsException("Facebook profile response was empty");
            }
            String id = text(profile, "id");
            String email = text(profile, "email");
            if (id == null || email == null) {
                throw new BadCredentialsException("Facebook token is missing required identity claims");
            }
            return new FacebookOAuthPrincipal(
                    id,
                    email,
                    true,
                    text(profile, "first_name"),
                    text(profile, "last_name"),
                    profile.path("picture").path("data").path("url").asText(null)
            );
        } catch (BadCredentialsException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BadCredentialsException("Invalid Facebook access token", ex);
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }
}
