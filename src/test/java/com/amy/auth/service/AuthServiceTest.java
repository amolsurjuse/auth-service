package com.amy.auth.service;

import com.amy.auth.domain.Country;
import com.amy.auth.domain.RefreshToken;
import com.amy.auth.domain.Role;
import com.amy.auth.domain.User;
import com.amy.auth.repository.RefreshTokenRepository;
import com.amy.auth.repository.RoleRepository;
import com.amy.auth.repository.UserRepository;
import com.amy.auth.web.dto.AddressDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Test
    void registerThrowsWhenEmailExists() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        AuthService service = buildService(userRepository);

        assertThatThrownBy(() -> service.register("user@example.com", "password", "device"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void registerSavesUserAndIssuesTokens() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        com.amy.auth.repository.AddressRepository addressRepository = mock(com.amy.auth.repository.AddressRepository.class);
        com.amy.auth.repository.CountryRepository countryRepository = mock(com.amy.auth.repository.CountryRepository.class);
        RedisRefreshSessionStore refreshStore = mock(RedisRefreshSessionStore.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);
        JwtService jwtService = mock(JwtService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        Role role = new Role(UUID.randomUUID(), "USER");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("hash");
        when(tokenVersionService.getVersion(any())).thenReturn(1L);
        when(jwtService.generateAccessToken(any(), any(), anyLong(), any())).thenReturn("access");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                addressRepository,
                countryRepository,
                refreshStore,
                tokenVersionService,
                jwtService,
                authenticationManager,
                passwordEncoder,
                7
        );

        AddressDto addressDto = new AddressDto("street", "city", "state", "12345", "US");
        Country country = new Country(UUID.randomUUID(), "US", "United States");
        when(countryRepository.findByIsoCode("US")).thenReturn(Optional.of(country));

        AuthService.TokenPair pair = service.register("User@Example.com", "password", "device", "First", "Last", "+12345678901", addressDto);

        assertThat(pair.accessToken()).isEqualTo("access");
        assertThat(pair.refreshToken()).isNotBlank();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");

        ArgumentCaptor<RefreshToken> rtCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(rtCaptor.capture());

        String expectedHash = sha256Hex(pair.refreshToken());
        assertThat(rtCaptor.getValue().getTokenHash()).isEqualTo(expectedHash);
        verify(refreshStore).put(eq(expectedHash), any(), any());
    }

    @Test
    void loginAuthenticatesAndIssuesTokens() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RedisRefreshSessionStore refreshStore = mock(RedisRefreshSessionStore.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);
        JwtService jwtService = mock(JwtService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        User user = new User(UUID.randomUUID(), "user@example.com", "hash", true, OffsetDateTime.now());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(tokenVersionService.getVersion(any())).thenReturn(1L);
        when(jwtService.generateAccessToken(any(), any(), anyLong(), any())).thenReturn("access");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                mock(com.amy.auth.repository.AddressRepository.class),
                mock(com.amy.auth.repository.CountryRepository.class),
                refreshStore,
                tokenVersionService,
                jwtService,
                authenticationManager,
                passwordEncoder,
                7
        );

        AuthService.TokenPair pair = service.login("User@Example.com", "password", "device");

        assertThat(pair.accessToken()).isEqualTo("access");
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("password");
    }

    @Test
    void refreshValidatesAndRotates() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RedisRefreshSessionStore refreshStore = mock(RedisRefreshSessionStore.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);
        JwtService jwtService = mock(JwtService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        User user = new User(UUID.randomUUID(), "user@example.com", "hash", true, OffsetDateTime.now());
        String refreshPlain = "plain-refresh";
        String refreshHash = sha256Hex(refreshPlain);

        RefreshToken db = new RefreshToken(UUID.randomUUID(), user, "device", refreshHash,
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now());
        when(refreshTokenRepository.findByTokenHash(refreshHash)).thenReturn(Optional.of(db));
        when(refreshStore.getIfPresent(refreshHash)).thenReturn(
                new RedisRefreshSessionStore.RefreshSessionView(user.getId(), "device", db.getId(), db.getExpiresAt())
        );
        when(tokenVersionService.getVersion(any())).thenReturn(1L);
        when(jwtService.generateAccessToken(any(), any(), anyLong(), any())).thenReturn("access");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                mock(com.amy.auth.repository.AddressRepository.class),
                mock(com.amy.auth.repository.CountryRepository.class),
                refreshStore,
                tokenVersionService,
                jwtService,
                authenticationManager,
                passwordEncoder,
                7
        );

        AuthService.TokenPair pair = service.refresh(refreshPlain, "device");

        assertThat(pair.accessToken()).isEqualTo("access");
        verify(refreshStore).delete(refreshHash, user.getId(), "device");
    }

    private AuthService buildService(UserRepository userRepository) {
        return new AuthService(
                userRepository,
                mock(RoleRepository.class),
                mock(RefreshTokenRepository.class),
                mock(com.amy.auth.repository.AddressRepository.class),
                mock(com.amy.auth.repository.CountryRepository.class),
                mock(RedisRefreshSessionStore.class),
                mock(TokenVersionService.class),
                mock(JwtService.class),
                mock(AuthenticationManager.class),
                mock(PasswordEncoder.class),
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
