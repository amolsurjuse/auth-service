package com.electrahub.identity.service;

import com.electrahub.identity.domain.VerificationChallenge;
import com.electrahub.identity.exception.OtpRateLimitException;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.VerificationChallengeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpChallengeServiceTest {
    private static final String PEPPER = "12345678901234567890123456789012";

    @Test
    void issuedCodeIsHmacProtectedAndCanBeVerified() {
        VerificationChallengeRepository repository = mock(VerificationChallengeRepository.class);
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        NotificationEventPublisher publisher = mock(NotificationEventPublisher.class);
        UUID userId = UUID.randomUUID();
        UserServiceClient.UserPrincipal unverified = principal(userId, false);
        UserServiceClient.UserPrincipal verified = principal(userId, true);
        when(repository.findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                userId, OtpChallengeService.EMAIL_VERIFICATION, VerificationChallenge.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(repository.findAllByUserIdAndPurposeAndStatus(
                userId, OtpChallengeService.EMAIL_VERIFICATION, VerificationChallenge.Status.ACTIVE))
                .thenReturn(List.of());
        when(userServiceClient.markEmailVerified(userId)).thenReturn(verified);
        OtpChallengeService service = service(repository, userServiceClient, publisher);

        service.issueForRegistration(unverified);

        ArgumentCaptor<VerificationChallenge> challengeCaptor =
                ArgumentCaptor.forClass(VerificationChallenge.class);
        verify(repository).save(challengeCaptor.capture());
        VerificationChallenge challenge = challengeCaptor.getValue();
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(publisher).publish(
                eq("USER_EMAIL_OTP_REQUESTED"), eq(userId), eq("user@example.com"), payloadCaptor.capture());
        String code = String.valueOf(payloadCaptor.getValue().get("code"));
        assertThat(code).matches("\\d{6}");
        assertThat(challenge.getCodeHash()).doesNotContain(code);
        when(repository.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        UserServiceClient.UserPrincipal result = service.verify(userId, challenge.getId(), code);

        assertThat(result.isEmailVerified()).isTrue();
        assertThat(challenge.getStatus()).isEqualTo(VerificationChallenge.Status.VERIFIED);
        verify(userServiceClient).markEmailVerified(userId);
    }

    @Test
    void resendCooldownIsEnforced() {
        VerificationChallengeRepository repository = mock(VerificationChallengeRepository.class);
        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        NotificationEventPublisher publisher = mock(NotificationEventPublisher.class);
        UUID userId = UUID.randomUUID();
        UserServiceClient.UserPrincipal principal = principal(userId, false);
        when(repository.findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                userId, OtpChallengeService.EMAIL_VERIFICATION, VerificationChallenge.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(repository.findAllByUserIdAndPurposeAndStatus(
                userId, OtpChallengeService.EMAIL_VERIFICATION, VerificationChallenge.Status.ACTIVE))
                .thenReturn(List.of());
        OtpChallengeService service = service(repository, userServiceClient, publisher);
        service.issueForRegistration(principal);
        ArgumentCaptor<VerificationChallenge> challengeCaptor =
                ArgumentCaptor.forClass(VerificationChallenge.class);
        verify(repository).save(challengeCaptor.capture());
        when(userServiceClient.getPrincipal(userId)).thenReturn(principal);
        when(repository.findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                userId, OtpChallengeService.EMAIL_VERIFICATION, VerificationChallenge.Status.ACTIVE))
                .thenReturn(Optional.of(challengeCaptor.getValue()));

        assertThatThrownBy(() -> service.request(userId))
                .isInstanceOf(OtpRateLimitException.class);
    }

    private OtpChallengeService service(
            VerificationChallengeRepository repository,
            UserServiceClient userServiceClient,
            NotificationEventPublisher publisher
    ) {
        return new OtpChallengeService(repository, userServiceClient, publisher, PEPPER, 10, 60, 5);
    }

    private UserServiceClient.UserPrincipal principal(UUID userId, boolean verified) {
        return new UserServiceClient.UserPrincipal(
                userId, "user@example.com", true, verified, false, List.of("USER"));
    }
}
