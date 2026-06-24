package com.electrahub.identity.service;

import com.electrahub.identity.domain.OAuthIdentity;
import com.electrahub.identity.exception.ConflictException;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.OAuthIdentityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthLoginServiceTest {

    @Test
    void googleLoginUsesExistingIdentityAndIssuesElectraHubTokens() {
        GoogleOidcTokenVerifier verifier = mock(GoogleOidcTokenVerifier.class);
        OAuthIdentityRepository identityRepository = mock(OAuthIdentityRepository.class);
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        AuthService authService = mock(AuthService.class);

        UUID userId = UUID.randomUUID();
        OAuthIdentity identity = identity(userId);
        UserServiceClient.UserPrincipal principal = new UserServiceClient.UserPrincipal(
                userId, "driver@example.com", true, false, false, List.of("USER"));
        UserServiceClient.UserPrincipal verifiedPrincipal = new UserServiceClient.UserPrincipal(
                userId, "driver@example.com", true, true, false, List.of("USER"));

        when(verifier.verify("id-token", "nonce")).thenReturn(googlePrincipal());
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-subject"))
                .thenReturn(Optional.of(identity));
        when(identityRepository.save(identity)).thenReturn(identity);
        when(userServiceClient.getPrincipal(userId)).thenReturn(principal);
        when(userServiceClient.markEmailVerified(userId)).thenReturn(verifiedPrincipal);
        when(authService.issueTokensForPrincipal(verifiedPrincipal, "device-1"))
                .thenReturn(new AuthService.TokenPair("access", "refresh"));

        OAuthLoginService service = new OAuthLoginService(
                verifier, mock(FacebookOAuthTokenVerifier.class), identityRepository, userServiceClient, authService, true);

        AuthService.TokenPair pair = service.loginWithGoogle("id-token", "nonce", "device-1");

        assertThat(pair.accessToken()).isEqualTo("access");
        verify(userServiceClient, never()).register(any());
        verify(userServiceClient).markEmailVerified(userId);
        verify(identityRepository).save(identity);
    }

    @Test
    void googleLoginAutoProvisionsNewUserAndSavesIdentityLink() {
        GoogleOidcTokenVerifier verifier = mock(GoogleOidcTokenVerifier.class);
        OAuthIdentityRepository identityRepository = mock(OAuthIdentityRepository.class);
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        AuthService authService = mock(AuthService.class);

        UUID userId = UUID.randomUUID();
        UserServiceClient.UserPrincipal principal = new UserServiceClient.UserPrincipal(
                userId, "driver@example.com", true, false, false, List.of("USER"));
        UserServiceClient.UserPrincipal verifiedPrincipal = new UserServiceClient.UserPrincipal(
                userId, "driver@example.com", true, true, false, List.of("USER"));

        when(verifier.verify("id-token", null)).thenReturn(googlePrincipal());
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-subject"))
                .thenReturn(Optional.empty());
        when(userServiceClient.register(any())).thenReturn(principal);
        when(identityRepository.save(any(OAuthIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userServiceClient.getPrincipal(userId)).thenReturn(principal);
        when(userServiceClient.markEmailVerified(userId)).thenReturn(verifiedPrincipal);
        when(authService.issueTokensForPrincipal(verifiedPrincipal, "device-1"))
                .thenReturn(new AuthService.TokenPair("access", "refresh"));

        OAuthLoginService service = new OAuthLoginService(
                verifier, mock(FacebookOAuthTokenVerifier.class), identityRepository, userServiceClient, authService, true);

        AuthService.TokenPair pair = service.loginWithGoogle("id-token", null, "device-1");

        assertThat(pair.refreshToken()).isEqualTo("refresh");
        verify(userServiceClient).register(any(UserServiceClient.RegisterUserRequest.class));
        verify(userServiceClient).markEmailVerified(userId);
        verify(authService).issueTokensForPrincipal(verifiedPrincipal, "device-1");
    }

    @Test
    void googleLoginReturnsConflictWhenEmailAlreadyExistsWithoutLink() {
        GoogleOidcTokenVerifier verifier = mock(GoogleOidcTokenVerifier.class);
        OAuthIdentityRepository identityRepository = mock(OAuthIdentityRepository.class);
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        AuthService authService = mock(AuthService.class);

        when(verifier.verify("id-token", null)).thenReturn(googlePrincipal());
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-subject"))
                .thenReturn(Optional.empty());
        when(userServiceClient.register(any()))
                .thenThrow(new RestClientResponseException("conflict", 409, "Conflict", null, null, null));

        OAuthLoginService service = new OAuthLoginService(
                verifier, mock(FacebookOAuthTokenVerifier.class), identityRepository, userServiceClient, authService, true);

        assertThatThrownBy(() -> service.loginWithGoogle("id-token", null, "device-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already registered");

        verify(authService, never()).issueTokensForPrincipal(any(), eq("device-1"));
    }

    private GoogleOidcPrincipal googlePrincipal() {
        return new GoogleOidcPrincipal(
                "google-subject",
                "driver@example.com",
                true,
                "Driver",
                "Example",
                "https://example.com/picture.png"
        );
    }

    private OAuthIdentity identity(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new OAuthIdentity(
                UUID.randomUUID(),
                "GOOGLE",
                "google-subject",
                userId,
                "driver@example.com",
                true,
                "Driver",
                "Example",
                null,
                now.minusDays(1),
                now.minusDays(1)
        );
    }
}
