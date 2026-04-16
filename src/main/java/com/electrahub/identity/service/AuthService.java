package com.electrahub.identity.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);


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

    /**
     * Creates register for `AuthService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param email input consumed by register.
     * @param rawPassword input consumed by register.
     * @param deviceId input consumed by register.
     * @return result produced by register.
     */
    @Transactional
    public TokenPair register(String email, String rawPassword, String deviceId) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering AuthService#register");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering AuthService#register with debug context");
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

    /**
     * Executes login for `AuthService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param email input consumed by login.
     * @param rawPassword input consumed by login.
     * @param deviceId input consumed by login.
     * @return result produced by login.
     */
    @Transactional
    public TokenPair login(String email, String rawPassword, String deviceId) {
        try {
            var principal = userServiceClient.authenticate(new UserServiceClient.AuthenticateUserRequest(email, rawPassword));
            assertLoginAllowed(principal);
            return issueTokens(principal, deviceId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw new BadCredentialsException("Invalid credentials");
            }
            throw ex;
        }
    }

    /**
     * Updates refresh for `AuthService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param refreshPlain input consumed by refresh.
     * @param deviceId input consumed by refresh.
     * @return result produced by refresh.
     */
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
        assertLoginAllowed(principal);
        return issueTokens(principal, deviceId);
    }

    /**
     * Executes revoke refresh for user device for `AuthService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param userId input consumed by revokeRefreshForUserDevice.
     * @param deviceId input consumed by revokeRefreshForUserDevice.
     */
    @Transactional
    public void revokeRefreshForUserDevice(UUID userId, String deviceId) {
        // Immediate enforcement in Redis
        refreshStore.revokeAllForUserDevice(userId, deviceId);
        // Durable cleanup
        refreshTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
    }

    /**
     * Executes revoke all refresh for user for `AuthService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param userId input consumed by revokeAllRefreshForUser.
     */
    @Transactional
    public void revokeAllRefreshForUser(UUID userId) {
        refreshStore.revokeAllForUser(userId);
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Executes issue tokens for `AuthService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param principal input consumed by issueTokens.
     * @param deviceId input consumed by issueTokens.
     * @return result produced by issueTokens.
     */
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

    private void assertLoginAllowed(UserServiceClient.UserPrincipal principal) {
        if (!principal.enabled()) {
            throw new DisabledException("User is disabled");
        }
        if (principal.pendingDeletion()) {
            throw new DisabledException("User account is pending deletion");
        }
    }

    /**
     * Executes sha256 hex for `AuthService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param value input consumed by sha256Hex.
     * @return result produced by sha256Hex.
     */
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
