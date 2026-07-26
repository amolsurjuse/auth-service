package com.electrahub.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FacebookOAuthTokenVerifierTest {
    private MockRestServiceServer server;
    private FacebookOAuthTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://graph.facebook.test");
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new FacebookOAuthTokenVerifier(true, "app-123", "server-secret", builder.build());
    }

    @Test
    void verifiesTokenAudienceBeforeLoadingProfile() {
        server.expect(requestTo("https://graph.facebook.test/debug_token?input_token=user-token&access_token=app-123%7Cserver-secret"))
                .andExpect(queryParam("input_token", "user-token"))
                .andRespond(withSuccess("""
                        {"data":{"app_id":"app-123","type":"USER","is_valid":true,"user_id":"user-42"}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.test/me?fields=id,email,first_name,last_name,picture.type(large)&access_token=user-token&appsecret_proof="
                        + appSecretProof("server-secret", "user-token")))
                .andRespond(withSuccess("""
                        {
                          "id":"user-42",
                          "email":"driver@example.com",
                          "first_name":"Taylor",
                          "last_name":"Driver",
                          "picture":{"data":{"url":"https://images.example/avatar.jpg"}}
                        }
                        """, MediaType.APPLICATION_JSON));

        FacebookOAuthPrincipal principal = verifier.verify("user-token");

        assertThat(principal.subject()).isEqualTo("user-42");
        assertThat(principal.email()).isEqualTo("driver@example.com");
        assertThat(principal.emailVerified()).isTrue();
        assertThat(principal.pictureUrl()).isEqualTo("https://images.example/avatar.jpg");
        server.verify();
    }

    @Test
    void rejectsTokenIssuedForAnotherApplication() {
        server.expect(requestTo("https://graph.facebook.test/debug_token?input_token=user-token&access_token=app-123%7Cserver-secret"))
                .andRespond(withSuccess("""
                        {"data":{"app_id":"another-app","type":"USER","is_valid":true,"user_id":"user-42"}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("user-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Facebook access token is invalid for this application");
        server.verify();
    }

    @Test
    void rejectsProfileThatDoesNotMatchTokenSubject() {
        server.expect(requestTo("https://graph.facebook.test/debug_token?input_token=user-token&access_token=app-123%7Cserver-secret"))
                .andRespond(withSuccess("""
                        {"data":{"app_id":"app-123","type":"USER","is_valid":true,"user_id":"user-42"}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.test/me?fields=id,email,first_name,last_name,picture.type(large)&access_token=user-token&appsecret_proof="
                        + appSecretProof("server-secret", "user-token")))
                .andRespond(withSuccess("""
                        {"id":"user-99","email":"driver@example.com"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("user-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Facebook token is missing required identity claims");
        server.verify();
    }

    @Test
    void refusesToRunWhenServerCredentialsAreMissing() {
        FacebookOAuthTokenVerifier unconfigured = new FacebookOAuthTokenVerifier(
                true,
                "app-123",
                "",
                RestClient.create()
        );

        assertThatThrownBy(() -> unconfigured.verify("user-token"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("Facebook login is not configured");
    }

    private String appSecretProof(String secret, String accessToken) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
