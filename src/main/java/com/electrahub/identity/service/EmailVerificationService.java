package com.electrahub.identity.service;

import com.electrahub.identity.domain.EmailVerificationToken;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.EmailVerificationTokenRepository;
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
    private final OtpChallengeService otpChallengeService;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserServiceClient userServiceClient,
            NotificationEventPublisher notificationPublisher,
            OtpChallengeService otpChallengeService
    ) {
        this.tokenRepository = tokenRepository;
        this.userServiceClient = userServiceClient;
        this.notificationPublisher = notificationPublisher;
        this.otpChallengeService = otpChallengeService;
    }

    @Transactional
    public void sendVerification(UserServiceClient.UserPrincipal principal) {
        if (principal == null || principal.isEmailVerified()) {
            return;
        }
        otpChallengeService.issueForRegistration(principal);
    }

    @Transactional
    public void resend(String email) {
        try {
            UserServiceClient.UserPrincipal principal = userServiceClient.getPrincipalByEmail(email);
            otpChallengeService.issue(principal, true);
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

    public OtpChallengeService.ChallengeStatus otpStatus(UUID userId) {
        return otpChallengeService.status(userId);
    }

    public OtpChallengeService.ChallengeStatus requestOtp(UUID userId) {
        return otpChallengeService.request(userId);
    }

    public UserServiceClient.UserPrincipal verifyOtp(UUID userId, UUID challengeId, String code) {
        return otpChallengeService.verify(userId, challengeId, code);
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
