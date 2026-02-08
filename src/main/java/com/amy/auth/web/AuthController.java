package com.amy.auth.web;

import com.amy.auth.config.JwtAuthFilter;
import com.amy.auth.service.*;
import com.amy.auth.web.dto.*;
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

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final TokenDenylistService denylistService;
    private final TokenVersionService tokenVersionService;

    private final long refreshTtlDays;

    public AuthController(
            AuthService authService,
            CookieUtil cookieUtil,
            TokenDenylistService denylistService,
            TokenVersionService tokenVersionService,
            @org.springframework.beans.factory.annotation.Value("${app.security.jwt.refresh-token-ttl-days}") long refreshTtlDays
    ) {
        this.authService = authService;
        this.cookieUtil = cookieUtil;
        this.denylistService = denylistService;
        this.tokenVersionService = tokenVersionService;
        this.refreshTtlDays = refreshTtlDays;
    }

    public record AccessTokenResponse(String accessToken, String tokenType) {}

    @PostMapping("/register")
    public ResponseEntity<AccessTokenResponse> register(
            @Valid @RequestBody RegisterRequest req,
            @CookieValue(name = "did", required = false) String did
    ) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;

        AuthService.TokenPair pair = authService.register(
                req.email(),
                req.password(),
                deviceId,
                req.firstName(),
                req.lastName(),
                req.phoneNumber(),
                req.address()
        );
        Duration refreshTtl = Duration.ofDays(refreshTtlDays);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer"));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest req,
            @CookieValue(name = "did", required = false) String did
    ) {
        String deviceId = (did == null || did.isBlank()) ? UUID.randomUUID().toString() : did;

        AuthService.TokenPair pair = authService.login(req.email(), req.password(), deviceId);
        Duration refreshTtl = Duration.ofDays(refreshTtlDays);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildDeviceCookie(deviceId).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer"));
    }

    /**
     * CSRF-protected: client must send X-XSRF-TOKEN header from XSRF-TOKEN cookie.
     * Refresh token is read from HttpOnly cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = "__Host-rt", required = false) String refreshCookie,
            @CookieValue(name = "did", required = false) String deviceId
    ) {
        if (refreshCookie == null || refreshCookie.isBlank() || deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthService.TokenPair pair = authService.refresh(refreshCookie, deviceId);
        Duration refreshTtl = Duration.ofDays(refreshTtlDays);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshCookie(pair.refreshToken(), refreshTtl).toString())
                .body(new AccessTokenResponse(pair.accessToken(), "Bearer"));
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
