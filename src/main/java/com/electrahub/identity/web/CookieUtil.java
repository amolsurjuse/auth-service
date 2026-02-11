package com.electrahub.identity.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    private final String refreshName;
    private final String deviceName;
    private final String sameSite;

    public CookieUtil(
            @Value("${app.cookies.refresh-name}") String refreshName,
            @Value("${app.cookies.device-name}") String deviceName,
            @Value("${app.cookies.same-site}") String sameSite
    ) {
        this.refreshName = refreshName;
        this.deviceName = deviceName;
        this.sameSite = sameSite;
    }

    public String refreshName() { return refreshName; }
    public String deviceName() { return deviceName; }

    public ResponseCookie buildRefreshCookie(String refreshToken, Duration ttl) {
        return ResponseCookie.from(refreshName, refreshToken)
                .httpOnly(true)
                .secure(true)     // set false ONLY for localhost HTTP testing
                .path("/")        // required for __Host- cookie
                .sameSite(sameSite)
                .maxAge(ttl)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(refreshName, "")
                .httpOnly(true)
                .secure(true)     // set false ONLY for localhost HTTP testing
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie buildDeviceCookie(String deviceId) {
        return ResponseCookie.from(deviceName, deviceId)
                .httpOnly(false)
                .secure(true)     // set false ONLY for localhost HTTP testing
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ofDays(3650))
                .build();
    }
}

