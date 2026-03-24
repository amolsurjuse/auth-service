package com.electrahub.identity.web;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieUtilTest.class);


    /**
     * Creates build refresh cookie uses http only and max age for `CookieUtilTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web`.
     */
    @Test
    void buildRefreshCookieUsesHttpOnlyAndMaxAge() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering CookieUtilTest#buildRefreshCookieUsesHttpOnlyAndMaxAge");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering CookieUtilTest#buildRefreshCookieUsesHttpOnlyAndMaxAge with debug context");
        CookieUtil util = new CookieUtil("__Host-rt", "did", "Lax");

        ResponseCookie cookie = util.buildRefreshCookie("token", Duration.ofDays(1));

        assertThat(cookie.getName()).isEqualTo("__Host-rt");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(1));
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    /**
     * Removes clear refresh cookie expires immediately for `CookieUtilTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web`.
     */
    @Test
    void clearRefreshCookieExpiresImmediately() {
        CookieUtil util = new CookieUtil("__Host-rt", "did", "Lax");

        ResponseCookie cookie = util.clearRefreshCookie();

        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    /**
     * Creates build device cookie is not http only for `CookieUtilTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.web`.
     */
    @Test
    void buildDeviceCookieIsNotHttpOnly() {
        CookieUtil util = new CookieUtil("__Host-rt", "did", "Lax");

        ResponseCookie cookie = util.buildDeviceCookie("device");

        assertThat(cookie.getName()).isEqualTo("did");
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(3650));
    }
}
