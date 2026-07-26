package com.electrahub.identity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class FacebookOAuthTokenVerifier {
    private final boolean enabled;
    private final String appId;
    private final String appSecret;
    private final RestClient restClient;

    @Autowired
    public FacebookOAuthTokenVerifier(
            @Value("${app.security.oauth.facebook.enabled:false}") boolean enabled,
            @Value("${app.security.oauth.facebook.graph-base-url:https://graph.facebook.com}") String graphBaseUrl,
            @Value("${app.security.oauth.facebook.app-id:}") String appId,
            @Value("${app.security.oauth.facebook.app-secret:}") String appSecret
    ) {
        this(
                enabled,
                appId,
                appSecret,
                RestClient.builder().baseUrl(normalizeBaseUrl(graphBaseUrl)).build()
        );
    }

    FacebookOAuthTokenVerifier(
            boolean enabled,
            String appId,
            String appSecret,
            RestClient restClient
    ) {
        this.enabled = enabled;
        this.appId = appId;
        this.appSecret = appSecret;
        this.restClient = restClient;
    }

    public FacebookOAuthPrincipal verify(String accessToken) {
        if (!enabled) {
            throw new DisabledException("Facebook login is not enabled");
        }
        if (isBlank(appId) || isBlank(appSecret)) {
            throw new DisabledException("Facebook login is not configured");
        }
        if (isBlank(accessToken)) {
            throw new BadCredentialsException("Facebook access token is required");
        }
        try {
            String tokenUserId = verifyTokenAudience(accessToken);
            String appSecretProof = createAppSecretProof(accessToken);
            JsonNode profile = restClient.get()
                    .uri(
                            "/me?fields=id,email,first_name,last_name,picture.type(large)&access_token={accessToken}&appsecret_proof={appSecretProof}",
                            accessToken,
                            appSecretProof
                    )
                    .retrieve()
                    .body(JsonNode.class);
            if (profile == null) {
                throw new BadCredentialsException("Facebook profile response was empty");
            }
            String id = text(profile, "id");
            String email = text(profile, "email");
            if (!tokenUserId.equals(id) || email == null) {
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
            // Do not retain the HTTP exception because its URI contains the app credential.
            throw new BadCredentialsException("Invalid Facebook access token");
        }
    }

    private String verifyTokenAudience(String accessToken) {
        JsonNode response = restClient.get()
                .uri(
                        "/debug_token?input_token={inputToken}&access_token={appAccessToken}",
                        accessToken,
                        appId + "|" + appSecret
                )
                .retrieve()
                .body(JsonNode.class);
        JsonNode data = response == null ? null : response.path("data");
        if (data == null
                || data.isMissingNode()
                || !data.path("is_valid").asBoolean(false)
                || !appId.equals(text(data, "app_id"))) {
            throw new BadCredentialsException("Facebook access token is invalid for this application");
        }

        String tokenType = text(data, "type");
        String userId = text(data, "user_id");
        if (userId == null || (tokenType != null && !"USER".equals(tokenType))) {
            throw new BadCredentialsException("Facebook access token is not a user token");
        }
        return userId;
    }

    private String createAppSecretProof(String accessToken) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Facebook app secret proof could not be generated");
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
