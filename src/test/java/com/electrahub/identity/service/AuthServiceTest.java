package com.electrahub.identity.service;

import com.electrahub.identity.domain.RefreshToken;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.RefreshTokenRepository;
import com.electrahub.identity.web.dto.AddressDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Test
    void registerMapsDuplicateEmailConflictToValidationError() {
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        when(userServiceClient.register(any()))
                .thenThrow(new RestClientResponseException("conflict", 409, "Conflict", null, null, null));

        AuthService service = buildService(userServiceClient, mock(RefreshTokenRepository.class));

        assertThatThrownBy(() ->
                service.register("user@example.com", "password", "device", "First", "Last", "+123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void loginMapsUnauthorizedToBadCredentials() {
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        when(userServiceClient.authenticate(any()))
                .thenThrow(new RestClientResponseException("unauthorized", 401, "Unauthorized", null, null, null));

        AuthService service = buildService(userServiceClient, mock(RefreshTokenRepository.class));

        assertThatThrownBy(() -> service.login("user@example.com", "wrong", "device"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void loginThrowsWhenUserIsDisabled() {
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        when(userServiceClient.authenticate(any()))
                .thenReturn(new UserServiceClient.UserPrincipal(
                        UUID.randomUUID(), "user@example.com", false, List.of("USER")));

        AuthService service = buildService(userServiceClient, mock(RefreshTokenRepository.class));

        assertThatThrownBy(() -> service.login("user@example.com", "password", "device"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("User is disabled");
    }

    @Test
    void registerPersistsRefreshTokenAndCachesSession() {
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RedisRefreshSessionStore refreshStore = mock(RedisRefreshSessionStore.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);
        JwtService jwtService = mock(JwtService.class);

        UUID userId = UUID.randomUUID();
        when(userServiceClient.register(any()))
                .thenReturn(new UserServiceClient.UserPrincipal(userId, "user@example.com", true, List.of("USER")));
        when(tokenVersionService.getVersion(userId)).thenReturn(1L);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyLong(), anyList())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(
                userServiceClient,
                refreshTokenRepository,
                refreshStore,
                tokenVersionService,
                jwtService,
                7
        );

        AddressDto address = new AddressDto("street", "city", "state", "12345", "US");
        AuthService.TokenPair pair = service.register(
                "User@Example.com", "password", "device-1", "First", "Last", "+1234567890", address);

        assertThat(pair.accessToken()).isEqualTo("access-token");
        assertThat(pair.refreshToken()).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getDeviceId()).isEqualTo("device-1");
        assertThat(captor.getValue().getTokenHash()).isEqualTo(sha256Hex(pair.refreshToken()));

        verify(refreshStore).put(eq(sha256Hex(pair.refreshToken())), any(), any());
    }

    @Test
    void refreshRotatesTokenAndDeletesOldFromCache() {
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RedisRefreshSessionStore refreshStore = mock(RedisRefreshSessionStore.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);
        JwtService jwtService = mock(JwtService.class);

        UUID userId = UUID.randomUUID();
        String refreshPlain = "plain-refresh";
        String hash = sha256Hex(refreshPlain);

        RefreshToken current = new RefreshToken(
                UUID.randomUUID(),
                userId,
                "device-1",
                hash,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now()
        );

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(current));
        when(refreshStore.getIfPresent(hash))
                .thenReturn(new RedisRefreshSessionStore.RefreshSessionView(
                        userId, "device-1", current.getId(), current.getExpiresAt()));
        when(userServiceClient.getPrincipal(userId))
                .thenReturn(new UserServiceClient.UserPrincipal(userId, "user@example.com", true, List.of("USER")));
        when(tokenVersionService.getVersion(userId)).thenReturn(2L);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyLong(), anyList())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(
                userServiceClient,
                refreshTokenRepository,
                refreshStore,
                tokenVersionService,
                jwtService,
                7
        );

        AuthService.TokenPair rotated = service.refresh(refreshPlain, "device-1");

        assertThat(rotated.accessToken()).isEqualTo("access-token");
        verify(refreshStore).delete(hash, userId, "device-1");
        assertThat(current.isRevoked()).isTrue();
    }

    private AuthService buildService(UserServiceClient userServiceClient, RefreshTokenRepository refreshTokenRepository) {
        return new AuthService(
                userServiceClient,
                refreshTokenRepository,
                mock(RedisRefreshSessionStore.class),
                mock(TokenVersionService.class),
                mock(JwtService.class),
                7
        );
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
