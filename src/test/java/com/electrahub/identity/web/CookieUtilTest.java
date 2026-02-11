package com.electrahub.identity.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    @Test
    void buildRefreshCookieUsesHttpOnlyAndMaxAge() {
        CookieUtil util = new CookieUtil("__Host-rt", "did", "Lax");

        ResponseCookie cookie = util.buildRefreshCookie("token", Duration.ofDays(1));

        assertThat(cookie.getName()).isEqualTo("__Host-rt");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(1));
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void clearRefreshCookieExpiresImmediately() {
        CookieUtil util = new CookieUtil("__Host-rt", "did", "Lax");

        ResponseCookie cookie = util.clearRefreshCookie();

        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    void buildDeviceCookieIsNotHttpOnly() {
        CookieUtil util = new CookieUtil("__Host-rt", "did", "Lax");

        ResponseCookie cookie = util.buildDeviceCookie("device");

        assertThat(cookie.getName()).isEqualTo("did");
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(3650));
    }
}
