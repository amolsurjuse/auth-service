package com.electrahub.identity.web;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.identity.config.JwtAuthFilter;
import com.electrahub.identity.service.*;
import com.electrahub.identity.web.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);


    private final AuthService authService;
    private final OAuthLoginService oauthLoginService;
    private final CookieUtil cookieUtil;
    private final TokenDenylistService denylistService;
    private final TokenVersionService tokenVersionService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    private final long refreshTtlDays;
    private final long webRefreshTtlHours;

    AuthController(
            AuthService authService,
            OAuthLoginService oauthLoginService,
            CookieUtil cookieUtil,
            TokenDenylistService denylistService,
            TokenVersionService tokenVersionService,
            PasswordResetService passwordResetService,
            EmailVerificationService emailVerificationService,
            long refreshTtlDays
    ) {
        this(
                authService,
                oauthLoginService,
                cookieUtil,
                denylistService,
                tokenVersionService,
                passwordResetService,
                emailVerificationService,
                refreshTtlDays,
                24
        );
    }

    public AuthController(
            AuthService authService,
            OAuthLoginService oauthLoginService,
            CookieUtil cookieUtil,
            TokenDenylistService denylistService,
            TokenVersionService tokenVersionService,
            PasswordResetService passwordResetService,
            EmailVerificationService emailVerificationService,
            @org.springframework.beans.factory.annotation.Value("${app.security.jwt.refresh-token-ttl-days}") long refreshTtlDays,
            @org.springframework.beans.factory.annotation.Value("${app.security.jwt.refresh-token-web-ttl-hours:24}") long webRefreshTtlHours
    ) {
        this.authService = authService;
        this.oauthLoginService = oauthLoginService;
        this.cookieUtil = cookieUtil;
        this.denylistService = denylistService;
        this.tokenVersionService = tokenVersionService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTtlDays = refreshTtlDays;
        this.webRefreshTtlHours = webRefreshTtlHours;
    }

    public record AccessTokenResponse(String accessToken, String tokenType, String refreshToken, String deviceId) {
        public AccessTokenResponse(String accessToken, String tokenType) {
            this(accessToken, tokenType, null, null);
        }
    }
    public record RefreshRequest(String refreshToken, String deviceId) {}
    public record AcceptedResponse(String status, String message) {}

    @PostMapping("/oauth/google")
    public ResponseEntity<AccessTokenResponse> googleLogin(
            @Valid @RequestBody GoogleOidcLoginRequest req,
            @CookieValue(name = "did", required = false) String did,
            HttpServletRequest request
    ) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;
        Duration refreshTtl = refreshTtlFor(request);

        AuthService.TokenPair pair = oauthLoginService.loginWithGoogle(req.idToken(), req.nonce(), deviceId, refreshTtl);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    ResponseEntity<AccessTokenResponse> googleLogin(GoogleOidcLoginRequest req, String did) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;
        Duration refreshTtl = Duration.ofDays(refreshTtlDays);
        AuthService.TokenPair pair = oauthLoginService.loginWithGoogle(req.idToken(), req.nonce(), deviceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    @PostMapping("/register")
    public ResponseEntity<AccessTokenResponse> register(
            @Valid @RequestBody RegisterRequest req,
            @CookieValue(name = "did", required = false) String did,
            HttpServletRequest request
    ) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;
        Duration refreshTtl = refreshTtlFor(request);

        AuthService.TokenPair pair = authService.register(
                req.email(),
                req.password(),
                deviceId,
                req.firstName(),
                req.lastName(),
                req.phoneNumber(),
                req.address(),
                refreshTtl
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    ResponseEntity<AccessTokenResponse> register(RegisterRequest req, String did) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;
        Duration refreshTtl = Duration.ofDays(refreshTtlDays);
        AuthService.TokenPair pair = authService.register(
                req.email(),
                req.password(),
                deviceId,
                req.firstName(),
                req.lastName(),
                req.phoneNumber(),
                req.address()
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest req,
            @CookieValue(name = "did", required = false) String did,
            HttpServletRequest request
    ) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;
        Duration refreshTtl = refreshTtlFor(request);

        AuthService.TokenPair pair = authService.login(req.email(), req.password(), deviceId, refreshTtl);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    ResponseEntity<AccessTokenResponse> login(LoginRequest req, String did) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;
        Duration refreshTtl = Duration.ofDays(refreshTtlDays);
        AuthService.TokenPair pair = authService.login(req.email(), req.password(), deviceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    @PostMapping("/oauth/facebook")
    public ResponseEntity<AccessTokenResponse> facebookLogin(
            @Valid @RequestBody SocialOAuthLoginRequest req,
            @CookieValue(name = "did", required = false) String did,
            HttpServletRequest request
    ) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;
        Duration refreshTtl = refreshTtlFor(request);

        AuthService.TokenPair pair = oauthLoginService.loginWithFacebook(req.accessToken(), deviceId, refreshTtl);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    ResponseEntity<AccessTokenResponse> facebookLogin(SocialOAuthLoginRequest req, String did) {
        return facebookLogin(req, did, null);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AcceptedResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.requestReset(req.email());
        return ResponseEntity.accepted().body(new AcceptedResponse(
                "ACCEPTED",
                "If the account exists, a password reset link will be sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AcceptedResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.ok(new AcceptedResponse("OK", "Password has been reset."));
    }

    @PostMapping("/email-verification/verify")
    public ResponseEntity<AcceptedResponse> verifyEmail(@Valid @RequestBody EmailVerificationRequest req) {
        emailVerificationService.verify(req.token());
        return ResponseEntity.ok(new AcceptedResponse("OK", "Email address has been verified."));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<AcceptedResponse> resendEmailVerification(@Valid @RequestBody ResendEmailVerificationRequest req) {
        emailVerificationService.resend(req.email());
        return ResponseEntity.accepted().body(new AcceptedResponse(
                "ACCEPTED",
                "If the account exists and is not verified, a verification email will be sent."
        ));
    }

    /**
     * CSRF-protected: client must send X-XSRF-TOKEN header from XSRF-TOKEN cookie.
     * Refresh token is read from HttpOnly cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = "__Host-rt", required = false) String refreshCookie,
            @CookieValue(name = "did", required = false) String deviceCookie,
            @RequestBody(required = false) RefreshRequest req,
            HttpServletRequest request
    ) {
        String bodyRefreshToken = req == null ? null : req.refreshToken();
        String bodyDeviceId = req == null ? null : req.deviceId();
        String refreshToken = firstNonBlank(bodyRefreshToken, refreshCookie);
        String deviceId = firstNonBlank(bodyDeviceId, deviceCookie);
        if (refreshToken == null || deviceId == null) {
            LOGGER.warn("Refresh request rejected: missing refresh token or device id bodyRefresh={} cookieRefresh={} bodyDevice={} cookieDevice={}",
                    hasText(bodyRefreshToken), hasText(refreshCookie), hasText(bodyDeviceId), hasText(deviceCookie));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Duration refreshTtl = refreshTtlFor(request);
        AuthService.TokenPair pair = authService.refreshWithFallback(refreshToken, deviceId, refreshCookie, deviceCookie, refreshTtl);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    ResponseEntity<AccessTokenResponse> refresh(String refreshCookie, String deviceCookie, RefreshRequest req) {
        String bodyRefreshToken = req == null ? null : req.refreshToken();
        String bodyDeviceId = req == null ? null : req.deviceId();
        String refreshToken = firstNonBlank(bodyRefreshToken, refreshCookie);
        String deviceId = firstNonBlank(bodyDeviceId, deviceCookie);
        if (refreshToken == null || deviceId == null) {
            LOGGER.warn("Refresh request rejected: missing refresh token or device id bodyRefresh={} cookieRefresh={} bodyDevice={} cookieDevice={}",
                    hasText(bodyRefreshToken), hasText(refreshCookie), hasText(bodyDeviceId), hasText(deviceCookie));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Duration refreshTtl = Duration.ofDays(refreshTtlDays);
        AuthService.TokenPair pair = authService.refreshWithFallback(refreshToken, deviceId, refreshCookie, deviceCookie);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer", pair.refreshToken(), deviceId));
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Duration refreshTtlFor(HttpServletRequest request) {
        return isMobileClient(request)
                ? Duration.ofDays(refreshTtlDays)
                : Duration.ofHours(webRefreshTtlHours);
    }

    private static boolean isMobileClient(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String clientType = request.getHeader("X-Client-Type");
        if (clientType != null) {
            String normalized = clientType.trim().toLowerCase(java.util.Locale.ROOT);
            if (normalized.equals("ios") || normalized.equals("android") || normalized.equals("mobile")) {
                return true;
            }
            if (normalized.equals("web") || normalized.equals("admin-web") || normalized.equals("driver-web")) {
                return false;
            }
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return false;
        }
        String normalizedUserAgent = userAgent.toLowerCase(java.util.Locale.ROOT);
        return normalizedUserAgent.contains("iphone")
                || normalizedUserAgent.contains("ipad")
                || normalizedUserAgent.contains("android")
                || normalizedUserAgent.contains("mobile");
    }

    /**
     * Logout current device:
     * - revoke refresh sessions for this device
     * - denylist current access token jti (single-token revocation) with TTL = remaining token lifetime
     */
    @PostMapping("/logout-device")
    public ResponseEntity<Void> logoutDevice(
            HttpServletRequest request,
            @CookieValue(name = "did", required = false) String deviceId
    ) {
        String uid = (String) request.getAttribute("uid");
        String jti = (String) request.getAttribute("jti");
        Date exp = (Date) request.getAttribute("exp");

        if (uid != null && deviceId != null && !deviceId.isBlank()) {
            authService.revokeRefreshForUserDevice(UUID.fromString(uid), deviceId);
        }

        if (jti != null && exp != null) {
            denylistService.deny(jti, JwtAuthFilter.remainingTtl(exp));
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearRefreshCookie().toString())
                .build();
    }

    /**
     * Logout all devices:
     * - revoke all refresh tokens
     * - bump token version (invalidates all access tokens immediately)
     * - denylist current jti as well (immediate for this token even if version read is delayed)
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletRequest request) {
        LOGGER.info(" Entering AuthController#logoutAll");
        LOGGER.debug(" Entering AuthController#logoutAll with debug context");
        String uid = (String) request.getAttribute("uid");
        String jti = (String) request.getAttribute("jti");
        Date exp = (Date) request.getAttribute("exp");

        if (uid != null) {
            UUID userId = UUID.fromString(uid);
            authService.revokeAllRefreshForUser(userId);
            tokenVersionService.bumpVersion(userId);
        }

        if (jti != null && exp != null) {
            denylistService.deny(jti, JwtAuthFilter.remainingTtl(exp));
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearRefreshCookie().toString())
                .build();
    }
}
