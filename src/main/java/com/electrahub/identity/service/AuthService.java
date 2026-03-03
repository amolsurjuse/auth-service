package com.electrahub.identity.service;

import com.electrahub.identity.domain.RefreshToken;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.RefreshTokenRepository;
import com.electrahub.identity.web.dto.AddressDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final RefreshTokenRepository refreshTokenRepository;

    private final RedisRefreshSessionStore refreshStore;
    private final TokenVersionService tokenVersionService;

    private final JwtService jwtService;

    private final long refreshTtlDays;

    public AuthService(
            UserServiceClient userServiceClient,
            RefreshTokenRepository refreshTokenRepository,
            RedisRefreshSessionStore refreshStore,
            TokenVersionService tokenVersionService,
            JwtService jwtService,
            @Value("${app.security.jwt.refresh-token-ttl-days}") long refreshTtlDays
    ) {
        this.userServiceClient = userServiceClient;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshStore = refreshStore;
        this.tokenVersionService = tokenVersionService;
        this.jwtService = jwtService;
        this.refreshTtlDays = refreshTtlDays;
    }

    public record TokenPair(String accessToken, String refreshToken) {}

    @Transactional
    public TokenPair register(String email, String rawPassword, String deviceId) {
        try {
            var principal = userServiceClient.register(new UserServiceClient.RegisterUserRequest(
                    email,
                    rawPassword,
                    null,
                    null,
                    null,
                    null
            ));
            return issueTokens(principal, deviceId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new IllegalArgumentException("Email already registered");
            }
            throw ex;
        }
    }

    @Transactional
    public TokenPair register(String email, String rawPassword, String deviceId,
                              String firstName, String lastName, String phoneNumber, AddressDto addressDto) {
        try {
            var principal = userServiceClient.register(new UserServiceClient.RegisterUserRequest(
                    email,
                    rawPassword,
                    firstName,
                    lastName,
                    phoneNumber,
                    addressDto
            ));
            return issueTokens(principal, deviceId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new IllegalArgumentException("Email already registered");
            }
            throw ex;
        }
    }

    @Transactional
    public TokenPair login(String email, String rawPassword, String deviceId) {
        try {
            var principal = userServiceClient.authenticate(new UserServiceClient.AuthenticateUserRequest(email, rawPassword));
            if (!principal.enabled()) {
                throw new DisabledException("User is disabled");
            }
            return issueTokens(principal, deviceId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw new BadCredentialsException("Invalid credentials");
            }
            throw ex;
        }
    }

    @Transactional
    public TokenPair refresh(String refreshPlain, String deviceId) {
        String hash = sha256Hex(refreshPlain);

        // Fast-path: Redis view (device binding)
        var view = refreshStore.getIfPresent(hash);

        RefreshToken db = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (db.isRevoked() || db.isExpiredNow()) {
            // cleanup best-effort
            refreshStore.delete(hash, db.getUserId(), db.getDeviceId());
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        // Device binding check
        String expectedDevice = db.getDeviceId();
        if (!expectedDevice.equals(deviceId)) {
            throw new IllegalArgumentException("Refresh token device mismatch");
        }
        if (view != null && !view.deviceId().equals(deviceId)) {
            throw new IllegalArgumentException("Refresh token device mismatch");
        }

        // Rotate: revoke old
        db.revoke();
        refreshStore.delete(hash, db.getUserId(), deviceId);

        var principal = userServiceClient.getPrincipal(db.getUserId());
        if (!principal.enabled()) {
            throw new DisabledException("User is disabled");
        }
        return issueTokens(principal, deviceId);
    }

    @Transactional
    public void revokeRefreshForUserDevice(UUID userId, String deviceId) {
        // Immediate enforcement in Redis
        refreshStore.revokeAllForUserDevice(userId, deviceId);
        // Durable cleanup
        refreshTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
    }

    @Transactional
    public void revokeAllRefreshForUser(UUID userId) {
        refreshStore.revokeAllForUser(userId);
        refreshTokenRepository.deleteByUserId(userId);
    }

    private TokenPair issueTokens(UserServiceClient.UserPrincipal principal, String deviceId) {
        long tv = tokenVersionService.getVersion(principal.userId());
        String access = jwtService.generateAccessToken(principal.email(), principal.userId().toString(), tv, principal.roles());

        String refreshPlain = UUID.randomUUID() + "." + UUID.randomUUID();
        String refreshHash = sha256Hex(refreshPlain);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime exp = now.plusDays(refreshTtlDays);
        Duration ttl = Duration.ofDays(refreshTtlDays);

        RefreshToken rt = new RefreshToken(UUID.randomUUID(), principal.userId(), deviceId, refreshHash, exp, now);
        refreshTokenRepository.save(rt);

        refreshStore.put(
                refreshHash,
                new RedisRefreshSessionStore.RefreshSessionView(principal.userId(), deviceId, rt.getId(), exp),
                ttl
        );

        return new TokenPair(access, refreshPlain);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash", e);
        }
    }

}
