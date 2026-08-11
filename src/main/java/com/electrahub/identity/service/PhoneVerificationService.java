package com.electrahub.identity.service;

import com.electrahub.identity.domain.VerificationChallenge;
import com.electrahub.identity.exception.OtpRateLimitException;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.VerificationChallengeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhoneVerificationService {
    private static final String PURPOSE = "PHONE_VERIFICATION";
    private static final String CHANNEL = "SMS";

    private final VerificationChallengeRepository repository;
    private final UserServiceClient users;
    private final NotificationEventPublisher notifications;
    private final SecureRandom random = new SecureRandom();
    private final byte[] pepper;
    private final long ttlMinutes;
    private final long resendCooldownSeconds;
    private final int maxAttempts;

    public PhoneVerificationService(
            VerificationChallengeRepository repository,
            UserServiceClient users,
            NotificationEventPublisher notifications,
            @Value("${app.otp.pepper}") String pepper,
            @Value("${app.otp.ttl-minutes:10}") long ttlMinutes,
            @Value("${app.otp.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${app.otp.max-attempts:5}") int maxAttempts) {
        this.repository = repository;
        this.users = users;
        this.notifications = notifications;
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
        this.ttlMinutes = ttlMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public ChallengeStatus request(UUID userId) {
        UserServiceClient.PhoneVerificationContact contact = users.getPhoneVerificationContact(userId);
        if (contact == null || contact.phoneNumber() == null || contact.phoneNumber().isBlank()) {
            throw new IllegalArgumentException("A phone number is required");
        }
        if (contact.phoneVerified()) return ChallengeStatus.verified(mask(contact.phoneNumber()));
        OffsetDateTime now = OffsetDateTime.now();
        repository.findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                userId, PURPOSE, VerificationChallenge.Status.ACTIVE).ifPresent(active -> {
            OffsetDateTime available = active.getCreatedAt().plusSeconds(resendCooldownSeconds);
            if (available.isAfter(now) && !active.isExpired(now)) {
                throw new OtpRateLimitException(Duration.between(now, available).toSeconds() + 1);
            }
        });
        repository.findAllByUserIdAndPurposeAndStatus(userId, PURPOSE, VerificationChallenge.Status.ACTIVE)
                .forEach(challenge -> challenge.supersede(now));
        String code = "%06d".formatted(random.nextInt(1_000_000));
        UUID id = UUID.randomUUID();
        VerificationChallenge challenge = new VerificationChallenge(id, userId, PURPOSE, CHANNEL,
                digest("destination", contact.phoneNumber()), mask(contact.phoneNumber()),
                digest(id + ":" + PURPOSE + ":" + CHANNEL, code), maxAttempts,
                now.plusMinutes(ttlMinutes), now);
        repository.save(challenge);
        notifications.publish("USER_PHONE_OTP_REQUESTED", userId, contact.phoneNumber(),
                Map.of("challengeId", id.toString(), "code", code, "expiresInMinutes", ttlMinutes));
        return status(challenge, now);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verify(UUID userId, UUID challengeId, String code) {
        VerificationChallenge challenge = repository.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));
        OffsetDateTime now = OffsetDateTime.now();
        if (!challenge.getUserId().equals(userId) || !PURPOSE.equals(challenge.getPurpose())
                || challenge.getStatus() != VerificationChallenge.Status.ACTIVE || challenge.isExpired(now)) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }
        String actual = digest(challengeId + ":" + PURPOSE + ":" + CHANNEL, code);
        if (!MessageDigest.isEqual(challenge.getCodeHash().getBytes(StandardCharsets.US_ASCII), actual.getBytes(StandardCharsets.US_ASCII))) {
            challenge.recordFailure(now);
            throw new IllegalArgumentException(challenge.getStatus() == VerificationChallenge.Status.LOCKED
                    ? "Too many attempts. Request a new verification code." : "Invalid verification code");
        }
        challenge.verify(now);
        users.markPhoneVerified(userId);
        notifications.publish("USER_PHONE_VERIFIED", userId, null, Map.of());
    }

    private ChallengeStatus status(VerificationChallenge value, OffsetDateTime now) {
        return new ChallengeStatus(false, value.getId(), value.getMaskedDestination(),
                value.isExpired(now) ? "EXPIRED" : value.getStatus().name(), value.getExpiresAt(),
                value.getCreatedAt().plusSeconds(resendCooldownSeconds), value.attemptsRemaining());
    }

    private String digest(String context, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((context + ":" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("HMAC-SHA-256 unavailable", exception); }
    }

    private static String mask(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() < 4 ? "***" : "*** *** " + digits.substring(digits.length() - 4);
    }

    public record ChallengeStatus(boolean phoneVerified, UUID challengeId, String maskedDestination,
            String status, OffsetDateTime expiresAt, OffsetDateTime resendAvailableAt, int attemptsRemaining) {
        static ChallengeStatus verified(String destination) {
            return new ChallengeStatus(true, null, destination, "VERIFIED", null, null, 0);
        }
    }
}
