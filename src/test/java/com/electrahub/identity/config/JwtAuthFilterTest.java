package com.electrahub.identity.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.identity.service.JwtService;
import com.electrahub.identity.service.TokenDenylistService;
import com.electrahub.identity.service.TokenVersionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthFilterTest.class);


    /**
     * Removes clear context for `JwtAuthFilterTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @AfterEach
    void clearContext() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering JwtAuthFilterTest#clearContext");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering JwtAuthFilterTest#clearContext with debug context");
        SecurityContextHolder.clearContext();
    }

    /**
     * Executes no authorization header skips authentication for `JwtAuthFilterTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void noAuthorizationHeaderSkipsAuthentication() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenDenylistService denylist = mock(TokenDenylistService.class);
        TokenVersionService versions = mock(TokenVersionService.class);

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, denylist, versions);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        verifyNoInteractions(jwtService, denylist, versions);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * Executes valid token authenticates and sets attributes for `JwtAuthFilterTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void validTokenAuthenticatesAndSetsAttributes() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenDenylistService denylist = mock(TokenDenylistService.class);
        TokenVersionService versions = mock(TokenVersionService.class);

        String token = "tkn";
        String email = "user@example.com";
        String uid = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();
        long tv = 2L;
        Date exp = Date.from(Instant.now().plusSeconds(60));

        when(jwtService.parseAndValidate(token))
                .thenReturn(new JwtService.ParsedToken(email, jti, uid, tv, exp, java.util.List.of("USER")));
        when(jwtService.isNotExpired(exp)).thenReturn(true);
        when(denylist.isDenied(jti)).thenReturn(false);
        when(versions.getVersion(UUID.fromString(uid))).thenReturn(tv);

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, denylist, versions);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(req.getAttribute("uid")).isEqualTo(uid);
        assertThat(req.getAttribute("jti")).isEqualTo(jti);
        assertThat(req.getAttribute("exp")).isEqualTo(exp);
    }

    /**
     * Executes remaining ttl is non negative for `JwtAuthFilterTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     */
    @Test
    void remainingTtlIsNonNegative() {
        Date exp = Date.from(Instant.now().plusSeconds(5));
        assertThat(JwtAuthFilter.remainingTtl(exp).toSeconds()).isGreaterThanOrEqualTo(0);
    }
}
