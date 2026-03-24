package com.electrahub.identity.web;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieUtil.class);


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

    /**
     * Creates build refresh cookie for `CookieUtil`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web`.
     * @param refreshToken input consumed by buildRefreshCookie.
     * @param ttl input consumed by buildRefreshCookie.
     * @return result produced by buildRefreshCookie.
     */
    public ResponseCookie buildRefreshCookie(String refreshToken, Duration ttl) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering CookieUtil#buildRefreshCookie");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering CookieUtil#buildRefreshCookie with debug context");
        return ResponseCookie.from(refreshName, refreshToken)
                .httpOnly(true)
                .secure(true)     // set false ONLY for localhost HTTP testing
                .path("/")        // required for __Host- cookie
                .sameSite(sameSite)
                .maxAge(ttl)
                .build();
    }

    /**
     * Removes clear refresh cookie for `CookieUtil`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web`.
     * @return result produced by clearRefreshCookie.
     */
    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(refreshName, "")
                .httpOnly(true)
                .secure(true)     // set false ONLY for localhost HTTP testing
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)
                .build();
    }

    /**
     * Creates build device cookie for `CookieUtil`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web`.
     * @param deviceId input consumed by buildDeviceCookie.
     * @return result produced by buildDeviceCookie.
     */
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

