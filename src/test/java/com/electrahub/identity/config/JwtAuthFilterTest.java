package com.electrahub.identity.config;

import com.electrahub.identity.service.JwtService;
import com.electrahub.identity.service.TokenDenylistService;
import com.electrahub.identity.service.TokenVersionService;
import com.electrahub.identity.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeaderSkipsAuthentication() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsServiceImpl uds = mock(UserDetailsServiceImpl.class);
        TokenDenylistService denylist = mock(TokenDenylistService.class);
        TokenVersionService versions = mock(TokenVersionService.class);

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, uds, denylist, versions);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        verifyNoInteractions(jwtService, uds, denylist, versions);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validTokenAuthenticatesAndSetsAttributes() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsServiceImpl uds = mock(UserDetailsServiceImpl.class);
        TokenDenylistService denylist = mock(TokenDenylistService.class);
        TokenVersionService versions = mock(TokenVersionService.class);

        String token = "tkn";
        String email = "user@example.com";
        String uid = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();
        long tv = 2L;
        Date exp = Date.from(Instant.now().plusSeconds(60));

        when(jwtService.parseAndValidate(token)).thenReturn(new JwtService.ParsedToken(email, jti, uid, tv, exp));
        when(jwtService.isNotExpired(exp)).thenReturn(true);
        when(denylist.isDenied(jti)).thenReturn(false);
        when(versions.getVersion(UUID.fromString(uid))).thenReturn(tv);

        when(uds.loadUserByUsername(email)).thenReturn(
                User.withUsername(email).password("x").authorities(List.of()).build()
        );

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, uds, denylist, versions);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(req.getAttribute("uid")).isEqualTo(uid);
        assertThat(req.getAttribute("jti")).isEqualTo(jti);
        assertThat(req.getAttribute("exp")).isEqualTo(exp);
    }

    @Test
    void remainingTtlIsNonNegative() {
        Date exp = Date.from(Instant.now().plusSeconds(5));
        assertThat(JwtAuthFilter.remainingTtl(exp).toSeconds()).isGreaterThanOrEqualTo(0);
    }
}
