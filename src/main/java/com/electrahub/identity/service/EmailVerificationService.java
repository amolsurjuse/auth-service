package com.electrahub.identity.service;

import com.electrahub.identity.domain.EmailVerificationToken;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.EmailVerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserServiceClient userServiceClient;
    private final NotificationEventPublisher notificationPublisher;
    private final String verificationUrl;
    private final long ttlMinutes;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserServiceClient userServiceClient,
            NotificationEventPublisher notificationPublisher,
            @Value("${app.notification.driver-verify-email-url:https://driver-portal.electrahub.net/verify-email}") String verificationUrl,
            @Value("${app.email-verification.ttl-minutes:1440}") long ttlMinutes
    ) {
        this.tokenRepository = tokenRepository;
        this.userServiceClient = userServiceClient;
        this.notificationPublisher = notificationPublisher;
        this.verificationUrl = verificationUrl;
        this.ttlMinutes = ttlMinutes;
    }

    @Transactional
    public void sendVerification(UserServiceClient.UserPrincipal principal) {
        if (principal == null || principal.emailVerified()) {
            return;
        }
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        tokenRepository.save(new EmailVerificationToken(
                UUID.randomUUID(),
                principal.userId(),
                principal.email(),
                sha256Hex(token),
                now.plusMinutes(ttlMinutes),
                now
        ));
        String url = verificationUrl + "?token=" + token;
        notificationPublisher.publish(
                "USER_EMAIL_VERIFICATION_REQUESTED",
                principal.userId(),
                principal.email(),
                Map.of("email", principal.email(), "verificationUrl", url)
        );
    }

    @Transactional
    public void resend(String email) {
        try {
            UserServiceClient.UserPrincipal principal = userServiceClient.getPrincipalByEmail(email);
            sendVerification(principal);
        } catch (RuntimeException ignored) {
            // Do not disclose whether an account exists.
        }
    }

    @Transactional
    public UserServiceClient.UserPrincipal verify(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByTokenHash(sha256Hex(token))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token"));
        OffsetDateTime now = OffsetDateTime.now();
        if (verificationToken.isUsed() || verificationToken.isExpired(now)) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }
        verificationToken.markUsed(now);
        UserServiceClient.UserPrincipal principal = userServiceClient.markEmailVerified(verificationToken.getUserId());
        notificationPublisher.publish(
                "USER_EMAIL_VERIFIED",
                principal.userId(),
                principal.email(),
                Map.of("email", principal.email())
        );
        return principal;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
