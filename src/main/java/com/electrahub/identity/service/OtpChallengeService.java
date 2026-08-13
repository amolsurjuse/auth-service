package com.electrahub.identity.service;

import com.electrahub.identity.domain.VerificationChallenge;
import com.electrahub.identity.exception.OtpRateLimitException;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.VerificationChallengeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class OtpChallengeService {
    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    public static final String EMAIL = "EMAIL";

    private final VerificationChallengeRepository repository;
    private final UserServiceClient userServiceClient;
    private final NotificationEventPublisher notificationPublisher;
    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] pepper;
    private final long ttlMinutes;
    private final long resendCooldownSeconds;
    private final int maxAttempts;

    public OtpChallengeService(
            VerificationChallengeRepository repository,
            UserServiceClient userServiceClient,
            NotificationEventPublisher notificationPublisher,
            @Value("${app.otp.pepper}") String pepper,
            @Value("${app.otp.ttl-minutes:10}") long ttlMinutes,
            @Value("${app.otp.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${app.otp.max-attempts:5}") int maxAttempts
    ) {
        if (pepper == null || pepper.length() < 32) {
            throw new IllegalStateException("OTP pepper must contain at least 32 characters");
        }
        this.repository = repository;
        this.userServiceClient = userServiceClient;
        this.notificationPublisher = notificationPublisher;
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
        this.ttlMinutes = ttlMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Transactional(readOnly = true)
    public ChallengeStatus status(UUID userId) {
        UserServiceClient.UserPrincipal principal = userServiceClient.getPrincipal(userId);
        if (principal.isEmailVerified()) {
            return ChallengeStatus.verified(maskEmail(principal.email()));
        }
        return repository.findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                        userId, EMAIL_VERIFICATION, VerificationChallenge.Status.ACTIVE)
                .map(challenge -> statusOf(challenge, OffsetDateTime.now()))
                .orElseGet(() -> ChallengeStatus.notIssued(maskEmail(principal.email())));
    }

    @Transactional
    public ChallengeStatus issueForRegistration(UserServiceClient.UserPrincipal principal) {
        return issue(principal, false);
    }

    @Transactional
    public ChallengeStatus request(UUID userId) {
        return issue(userServiceClient.getPrincipal(userId), true);
    }

    @Transactional
    public ChallengeStatus issue(UserServiceClient.UserPrincipal principal, boolean enforceCooldown) {
        if (principal == null) {
            throw new IllegalArgumentException("User account is unavailable");
        }
        if (principal.isEmailVerified()) {
            return ChallengeStatus.verified(maskEmail(principal.email()));
        }

        OffsetDateTime now = OffsetDateTime.now();
        repository.findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                principal.userId(), EMAIL_VERIFICATION, VerificationChallenge.Status.ACTIVE
        ).ifPresent(active -> {
            OffsetDateTime nextIssueAt = active.getCreatedAt().plusSeconds(resendCooldownSeconds);
            if (enforceCooldown && nextIssueAt.isAfter(now) && !active.isExpired(now)) {
                throw new OtpRateLimitException(Duration.between(now, nextIssueAt).toSeconds() + 1);
            }
        });

        repository.findAllByUserIdAndPurposeAndStatus(
                principal.userId(), EMAIL_VERIFICATION, VerificationChallenge.Status.ACTIVE
        ).forEach(challenge -> challenge.supersede(now));

        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        UUID challengeId = UUID.randomUUID();
        VerificationChallenge challenge = new VerificationChallenge(
                challengeId,
                principal.userId(),
                EMAIL_VERIFICATION,
                EMAIL,
                digest("destination", principal.email().trim().toLowerCase()),
                maskEmail(principal.email()),
                digest(challengeId + ":" + EMAIL_VERIFICATION + ":" + EMAIL, code),
                maxAttempts,
                now.plusMinutes(ttlMinutes),
                now
        );
        repository.save(challenge);

        notificationPublisher.publish(
                "USER_EMAIL_OTP_REQUESTED",
                principal.userId(),
                principal.email(),
                Map.of(
                        "challengeId", challengeId.toString(),
                        "code", code,
                        "expiresInMinutes", ttlMinutes
                )
        );
        return statusOf(challenge, now);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public UserServiceClient.UserPrincipal verify(UUID userId, UUID challengeId, String code) {
        VerificationChallenge challenge = repository.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));
        OffsetDateTime now = OffsetDateTime.now();
        if (!challenge.getUserId().equals(userId)
                || !EMAIL_VERIFICATION.equals(challenge.getPurpose())
                || challenge.getStatus() != VerificationChallenge.Status.ACTIVE
                || challenge.isExpired(now)) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        String expected = challenge.getCodeHash();
        String actual = digest(challengeId + ":" + EMAIL_VERIFICATION + ":" + EMAIL, code);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), actual.getBytes(StandardCharsets.US_ASCII))) {
            challenge.recordFailure(now);
            if (challenge.getStatus() == VerificationChallenge.Status.LOCKED) {
                throw new IllegalArgumentException("Too many attempts. Request a new verification code.");
            }
            throw new IllegalArgumentException("Invalid verification code");
        }

        challenge.verify(now);
        UserServiceClient.UserPrincipal principal = userServiceClient.markEmailVerified(userId);
        notificationPublisher.publish(
                "USER_EMAIL_VERIFIED",
                principal.userId(),
                principal.email(),
                Map.of("email", principal.email())
        );
        return principal;
    }

    private ChallengeStatus statusOf(VerificationChallenge challenge, OffsetDateTime now) {
        String status = challenge.isExpired(now) ? "EXPIRED" : challenge.getStatus().name();
        return new ChallengeStatus(
                false,
                challenge.getId(),
                challenge.getMaskedDestination(),
                status,
                challenge.getExpiresAt(),
                challenge.getCreatedAt().plusSeconds(resendCooldownSeconds),
                challenge.attemptsRemaining()
        );
    }

    private String digest(String context, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((context + ":" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA-256 unavailable", ex);
        }
    }

    static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "your email";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String visible = local.isEmpty() ? "*" : local.substring(0, 1);
        return visible + "***@" + parts[1];
    }

    public record ChallengeStatus(
            boolean emailVerified,
            UUID challengeId,
            String maskedDestination,
            String status,
            OffsetDateTime expiresAt,
            OffsetDateTime resendAvailableAt,
            int attemptsRemaining
    ) {
        static ChallengeStatus verified(String maskedDestination) {
            return new ChallengeStatus(true, null, maskedDestination, "VERIFIED", null, null, 0);
        }

        static ChallengeStatus notIssued(String maskedDestination) {
            return new ChallengeStatus(false, null, maskedDestination, "NOT_ISSUED", null, null, 0);
        }
    }
}
