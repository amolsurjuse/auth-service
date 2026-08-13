package com.electrahub.identity.service;

import com.electrahub.identity.integration.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserServiceClient userServiceClient;
    private final StringRedisTemplate redisTemplate;
    private final NotificationEventPublisher notificationPublisher;
    private final String resetPrefix;
    private final Duration resetTtl;
    private final String driverResetPasswordUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserServiceClient userServiceClient,
            StringRedisTemplate redisTemplate,
            NotificationEventPublisher notificationPublisher,
            @Value("${app.redis.password-reset-prefix}") String resetPrefix,
            @Value("${app.redis.password-reset-ttl-minutes}") long resetTtlMinutes,
            @Value("${app.notification.driver-reset-password-url}") String driverResetPasswordUrl
    ) {
        this.userServiceClient = userServiceClient;
        this.redisTemplate = redisTemplate;
        this.notificationPublisher = notificationPublisher;
        this.resetPrefix = resetPrefix;
        this.resetTtl = Duration.ofMinutes(Math.max(resetTtlMinutes, 5));
        this.driverResetPasswordUrl = driverResetPasswordUrl;
    }

    public void requestReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        try {
            UserServiceClient.UserPrincipal principal = userServiceClient.getPrincipalByEmail(normalizedEmail);
            String token = newToken();
            redisTemplate.opsForValue().set(resetPrefix + token, principal.userId().toString(), resetTtl);
            notificationPublisher.publish(
                    "USER_PASSWORD_RESET_REQUESTED",
                    principal.userId(),
                    principal.email(),
                    Map.of(
                            "email", principal.email(),
                            "resetToken", token,
                            "resetUrl", resetUrl(token),
                            "expiresInMinutes", resetTtl.toMinutes()
                    )
            );
        } catch (RestClientResponseException ex) {
            // Do not disclose whether an email exists.
            log.debug("Password reset request did not resolve to a user: status={}", ex.getStatusCode().value());
        } catch (RuntimeException ex) {
            log.warn("Password reset request could not be prepared for email hash={}", Integer.toHexString(normalizedEmail.hashCode()), ex);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String key = resetPrefix + token;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Password reset token is invalid or expired");
        }
        UUID userUuid = UUID.fromString(userId);
        userServiceClient.resetPassword(userUuid, new UserServiceClient.ResetPasswordRequest(newPassword));
        redisTemplate.delete(key);
        UserServiceClient.UserPrincipal principal = userServiceClient.getPrincipal(userUuid);
        notificationPublisher.publish("USER_PASSWORD_CHANGED", userUuid, principal.email(), Map.of("email", principal.email()));
    }

    private String resetUrl(String token) {
        String separator = driverResetPasswordUrl.contains("?") ? "&" : "?";
        return driverResetPasswordUrl + separator + "token=" + token;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
