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
    private final NotificationEventPublisher notificationEventPublisher;
    private final EmailVerificationService emailVerificationService;

    private final Duration defaultRefreshTtl;
    private final Duration refreshRotationGrace;

    AuthService(
            UserServiceClient userServiceClient,
            RefreshTokenRepository refreshTokenRepository,
            RedisRefreshSessionStore refreshStore,
            TokenVersionService tokenVersionService,
            JwtService jwtService,
            NotificationEventPublisher notificationEventPublisher,
            EmailVerificationService emailVerificationService,
            long refreshTtlDays
    ) {
        this(
                userServiceClient,
                refreshTokenRepository,
                refreshStore,
                tokenVersionService,
                jwtService,
                notificationEventPublisher,
                emailVerificationService,
                refreshTtlDays,
                60
        );
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(
            UserServiceClient userServiceClient,
            RefreshTokenRepository refreshTokenRepository,
            RedisRefreshSessionStore refreshStore,
            TokenVersionService tokenVersionService,
            JwtService jwtService,
            NotificationEventPublisher notificationEventPublisher,
            EmailVerificationService emailVerificationService,
            @Value("${app.security.jwt.refresh-token-ttl-days}") long refreshTtlDays,
            @Value("${app.security.jwt.refresh-token-rotation-grace-seconds:60}") long refreshRotationGraceSeconds
    ) {
        this.userServiceClient = userServiceClient;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshStore = refreshStore;
        this.tokenVersionService = tokenVersionService;
        this.jwtService = jwtService;
        this.notificationEventPublisher = notificationEventPublisher;
        this.emailVerificationService = emailVerificationService;
        this.defaultRefreshTtl = Duration.ofDays(refreshTtlDays);
        this.refreshRotationGrace = Duration.ofSeconds(Math.max(0, refreshRotationGraceSeconds));
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
        LOGGER.info(" Entering AuthService#register");
        LOGGER.debug(" Entering AuthService#register with debug context");
        try {
            var principal = userServiceClient.register(new UserServiceClient.RegisterUserRequest(
                    email,
                    rawPassword,
                    null,
                    null,
                    null,
                    null
            ));
            notificationEventPublisher.publish("USER_ACCOUNT_CREATED", principal.userId(), principal.email(), java.util.Map.of("email", principal.email()));
            emailVerificationService.sendVerification(principal);
            return issueTokens(principal, deviceId, defaultRefreshTtl);
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
        return register(email, rawPassword, deviceId, firstName, lastName, phoneNumber, addressDto, defaultRefreshTtl);
    }

    @Transactional
    public TokenPair register(String email, String rawPassword, String deviceId,
                              String firstName, String lastName, String phoneNumber, AddressDto addressDto,
                              Duration refreshTtl) {
        return register(email, rawPassword, deviceId, firstName, lastName, phoneNumber, addressDto, null, refreshTtl);
    }

    @Transactional
    public TokenPair register(String email, String rawPassword, String deviceId,
                              String firstName, String lastName, String phoneNumber, AddressDto addressDto,
                              String application, Duration refreshTtl) {
        try {
            var principal = userServiceClient.register(new UserServiceClient.RegisterUserRequest(
                    email,
                    rawPassword,
                    firstName,
                    lastName,
                    phoneNumber,
                    addressDto,
                    application
            ));
            notificationEventPublisher.publish(
                    "USER_ACCOUNT_CREATED",
                    principal.userId(),
                    principal.email(),
                    java.util.Map.of(
                            "email", principal.email(),
                            "firstName", firstName == null ? "" : firstName,
                            "lastName", lastName == null ? "" : lastName
                    )
            );
            emailVerificationService.sendVerification(principal);
            return issueTokens(principal, deviceId, refreshTtl);
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
        return login(email, rawPassword, deviceId, defaultRefreshTtl);
    }

    @Transactional
    public TokenPair login(String email, String rawPassword, String deviceId, Duration refreshTtl) {
        try {
            var principal = userServiceClient.authenticate(new UserServiceClient.AuthenticateUserRequest(email, rawPassword));
            assertLoginAllowed(principal);
            return issueTokens(principal, deviceId, refreshTtl);
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
        return refresh(refreshPlain, deviceId, defaultRefreshTtl);
    }

    @Transactional
    public TokenPair refresh(String refreshPlain, String deviceId, Duration refreshTtl) {
        String hash = sha256Hex(refreshPlain);

        // Fast-path: Redis view (device binding)
        var view = refreshStore.getIfPresent(hash);

        RefreshToken db = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (db.isExpiredNow()) {
            // cleanup best-effort
            refreshStore.delete(hash, db.getUserId(), db.getDeviceId());
            throw new IllegalArgumentException("Refresh token expired");
        }

        if (db.isRevoked()) {
            return refreshRecentlyRotatedToken(db, deviceId, refreshTtl)
                    .orElseThrow(() -> new IllegalArgumentException("Refresh token revoked"));
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
        return issueTokens(principal, deviceId, refreshTtl);
    }

    private java.util.Optional<TokenPair> refreshRecentlyRotatedToken(RefreshToken revokedToken, String deviceId, Duration refreshTtl) {
        if (refreshRotationGrace.isZero() || !revokedToken.getDeviceId().equals(deviceId)) {
            return java.util.Optional.empty();
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime graceStartedAt = now.minus(refreshRotationGrace);
        return refreshTokenRepository
                .findTopByUserIdAndDeviceIdAndRevokedFalseOrderByCreatedAtDesc(revokedToken.getUserId(), deviceId)
                .filter(activeToken -> activeToken.getCreatedAt().isAfter(revokedToken.getCreatedAt()))
                .filter(activeToken -> activeToken.getCreatedAt().isAfter(graceStartedAt))
                .map(activeToken -> {
                    LOGGER.info(
                            "Refresh token accepted within rotation grace userId={} deviceId={} activeTokenId={}",
                            revokedToken.getUserId(),
                            deviceId,
                            activeToken.getId()
                    );
                    activeToken.revoke();
                    refreshStore.delete(activeToken.getTokenHash(), activeToken.getUserId(), deviceId);
                    var principal = userServiceClient.getPrincipal(revokedToken.getUserId());
                    assertLoginAllowed(principal);
                    return issueTokens(principal, deviceId, refreshTtl);
                });
    }

    @Transactional
    public TokenPair refreshWithFallback(
            String primaryRefreshPlain,
            String primaryDeviceId,
            String fallbackRefreshPlain,
            String fallbackDeviceId
    ) {
        return refreshWithFallback(primaryRefreshPlain, primaryDeviceId, fallbackRefreshPlain, fallbackDeviceId, defaultRefreshTtl);
    }

    @Transactional
    public TokenPair refreshWithFallback(
            String primaryRefreshPlain,
            String primaryDeviceId,
            String fallbackRefreshPlain,
            String fallbackDeviceId,
            Duration refreshTtl
    ) {
        try {
            return refresh(primaryRefreshPlain, primaryDeviceId, refreshTtl);
        } catch (IllegalArgumentException primaryFailure) {
            if (!hasText(fallbackRefreshPlain)
                    || !hasText(fallbackDeviceId)
                    || sameRefreshAttempt(primaryRefreshPlain, primaryDeviceId, fallbackRefreshPlain, fallbackDeviceId)) {
                LOGGER.warn("Refresh token rejected source=primary reason={}", primaryFailure.getMessage());
                throw new BadCredentialsException("Invalid refresh token", primaryFailure);
            }

            LOGGER.warn("Refresh token rejected source=primary reason={}; trying fallback source", primaryFailure.getMessage());
            try {
                return refresh(fallbackRefreshPlain, fallbackDeviceId, refreshTtl);
            } catch (IllegalArgumentException fallbackFailure) {
                LOGGER.warn("Refresh token rejected source=fallback reason={}", fallbackFailure.getMessage());
                throw new BadCredentialsException("Invalid refresh token", fallbackFailure);
            }
        }
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
    @Transactional
    public TokenPair issueTokensForPrincipal(UserServiceClient.UserPrincipal principal, String deviceId) {
        return issueTokensForPrincipal(principal, deviceId, defaultRefreshTtl);
    }

    @Transactional
    public TokenPair issueTokensForPrincipal(UserServiceClient.UserPrincipal principal, String deviceId, Duration refreshTtl) {
        assertLoginAllowed(principal);
        return issueTokens(principal, deviceId, refreshTtl);
    }

    private TokenPair issueTokens(UserServiceClient.UserPrincipal principal, String deviceId, Duration refreshTtl) {
        long tv = tokenVersionService.getVersion(principal.userId());
        String access = jwtService.generateAccessToken(
                principal.email(), principal.userId().toString(), principal.effectiveTenantId(), tv, principal.roles());

        String refreshPlain = UUID.randomUUID() + "." + UUID.randomUUID();
        String refreshHash = sha256Hex(refreshPlain);

        OffsetDateTime now = OffsetDateTime.now();
        Duration ttl = refreshTtl == null || refreshTtl.isNegative() || refreshTtl.isZero()
                ? defaultRefreshTtl
                : refreshTtl;
        OffsetDateTime exp = now.plus(ttl);

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
        if (!principal.isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        if (principal.isPendingDeletion()) {
            throw new DisabledException("User account is pending deletion");
        }
    }

    private static boolean sameRefreshAttempt(String leftRefreshPlain, String leftDeviceId, String rightRefreshPlain, String rightDeviceId) {
        return java.util.Objects.equals(leftRefreshPlain, rightRefreshPlain)
                && java.util.Objects.equals(leftDeviceId, rightDeviceId);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
