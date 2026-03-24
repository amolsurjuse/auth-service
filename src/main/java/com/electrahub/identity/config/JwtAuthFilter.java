package com.electrahub.identity.config;


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.identity.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthFilter.class);


    private final JwtService jwtService;
    private final TokenDenylistService denylistService;
    private final TokenVersionService tokenVersionService;

    public JwtAuthFilter(
            JwtService jwtService,
            TokenDenylistService denylistService,
            TokenVersionService tokenVersionService
    ) {
        this.jwtService = jwtService;
        this.denylistService = denylistService;
        this.tokenVersionService = tokenVersionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            JwtService.ParsedToken parsed = jwtService.parseAndValidate(token);
            if (!jwtService.isNotExpired(parsed.exp())) {
                chain.doFilter(request, response);
                return;
            }

            if (denylistService.isDenied(parsed.jti())) {
                chain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(parsed.uid());
            long currentVersion = tokenVersionService.getVersion(userId);
            if (parsed.tv() != currentVersion) {
                chain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var authorities = parsed.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(parsed.subjectEmail(), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Pass useful info for logout endpoints
                request.setAttribute("uid", parsed.uid());
                request.setAttribute("jti", parsed.jti());
                request.setAttribute("exp", parsed.exp());
            }

        } catch (Exception ignored) {
            // invalid token -> unauthenticated
        }

        chain.doFilter(request, response);
    }

    /**
     * Executes remaining ttl for `JwtAuthFilter`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.config`.
     * @param exp input consumed by remainingTtl.
     * @return result produced by remainingTtl.
     */
    public static Duration remainingTtl(Date exp) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering JwtAuthFilter#remainingTtl");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering JwtAuthFilter#remainingTtl with debug context");
        long seconds = Math.max(0, exp.toInstant().getEpochSecond() - Instant.now().getEpochSecond());
        return Duration.ofSeconds(seconds);
    }
}
