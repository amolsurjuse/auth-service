package com.electrahub.identity.web;

import com.electrahub.identity.service.AuthService;
import com.electrahub.identity.service.TokenDenylistService;
import com.electrahub.identity.service.TokenVersionService;
import com.electrahub.identity.web.dto.AddressDto;
import com.electrahub.identity.web.dto.LoginRequest;
import com.electrahub.identity.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Test
    void registerSetsCookiesAndReturnsAccessToken() {
        AuthService authService = mock(AuthService.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        TokenDenylistService denylistService = mock(TokenDenylistService.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);

        when(authService.register(any(), any(), any(), any(), any(), any(), any())).thenReturn(new AuthService.TokenPair("access", "refresh"));
        when(cookieUtil.buildDeviceCookie(anyString())).thenReturn(ResponseCookie.from("did", "device").path("/").build());
        when(cookieUtil.buildRefreshCookie(anyString(), any())).thenReturn(ResponseCookie.from("__Host-rt", "refresh").path("/").build());

        AuthController controller = new AuthController(authService, cookieUtil, denylistService, tokenVersionService, 7);

        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "First", "Last", "+12345678901",
                new AddressDto("street", "city", "state", "12345", "US"));
        ResponseEntity<AuthController.AccessTokenResponse> response = controller.register(req, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).hasSize(2);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access");
    }

    @Test
    void loginSetsCookiesAndReturnsAccessToken() {
        AuthService authService = mock(AuthService.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        TokenDenylistService denylistService = mock(TokenDenylistService.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);

        when(authService.login(any(), any(), any())).thenReturn(new AuthService.TokenPair("access", "refresh"));
        when(cookieUtil.buildDeviceCookie(anyString())).thenReturn(ResponseCookie.from("did", "device").path("/").build());
        when(cookieUtil.buildRefreshCookie(anyString(), any())).thenReturn(ResponseCookie.from("__Host-rt", "refresh").path("/").build());

        AuthController controller = new AuthController(authService, cookieUtil, denylistService, tokenVersionService, 7);

        LoginRequest req = new LoginRequest("user@example.com", "password");
        ResponseEntity<AuthController.AccessTokenResponse> response = controller.login(req, "did");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
    }

    @Test
    void refreshWithoutCookiesReturnsUnauthorized() {
        AuthService authService = mock(AuthService.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        TokenDenylistService denylistService = mock(TokenDenylistService.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);

        AuthController controller = new AuthController(authService, cookieUtil, denylistService, tokenVersionService, 7);

        ResponseEntity<AuthController.AccessTokenResponse> response = controller.refresh(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutDeviceRevokesAndClearsCookie() {
        AuthService authService = mock(AuthService.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        TokenDenylistService denylistService = mock(TokenDenylistService.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);

        when(cookieUtil.clearRefreshCookie()).thenReturn(ResponseCookie.from("__Host-rt", "").path("/").build());

        AuthController controller = new AuthController(authService, cookieUtil, denylistService, tokenVersionService, 7);

        MockHttpServletRequest request = new MockHttpServletRequest();
        UUID userId = UUID.randomUUID();
        request.setAttribute("uid", userId.toString());
        request.setAttribute("jti", "jti");
        request.setAttribute("exp", Date.from(Instant.now().plusSeconds(60)));

        ResponseEntity<Void> response = controller.logoutDevice(request, "device");

        verify(authService).revokeRefreshForUserDevice(userId, "device");
        verify(denylistService).deny(eq("jti"), any(Duration.class));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void logoutAllRevokesAllAndBumpsVersion() {
        AuthService authService = mock(AuthService.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        TokenDenylistService denylistService = mock(TokenDenylistService.class);
        TokenVersionService tokenVersionService = mock(TokenVersionService.class);

        when(cookieUtil.clearRefreshCookie()).thenReturn(ResponseCookie.from("__Host-rt", "").path("/").build());

        AuthController controller = new AuthController(authService, cookieUtil, denylistService, tokenVersionService, 7);

        MockHttpServletRequest request = new MockHttpServletRequest();
        UUID userId = UUID.randomUUID();
        request.setAttribute("uid", userId.toString());
        request.setAttribute("jti", "jti");
        request.setAttribute("exp", Date.from(Instant.now().plusSeconds(60)));

        ResponseEntity<Void> response = controller.logoutAll(request);

        verify(authService).revokeAllRefreshForUser(userId);
        verify(tokenVersionService).bumpVersion(userId);
        verify(denylistService).deny(eq("jti"), any(Duration.class));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
