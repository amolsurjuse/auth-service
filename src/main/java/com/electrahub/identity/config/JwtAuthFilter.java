package com.electrahub.identity.config;


import com.electrahub.identity.service.*;
import com.electrahub.identity.service.impl.UserDetailsServiceImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenDenylistService denylistService;
    private final TokenVersionService tokenVersionService;

    public JwtAuthFilter(
            JwtService jwtService,
            UserDetailsServiceImpl userDetailsService,
            TokenDenylistService denylistService,
            TokenVersionService tokenVersionService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
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
                var userDetails = userDetailsService.loadUserByUsername(parsed.subjectEmail());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
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

    public static Duration remainingTtl(Date exp) {
        long seconds = Math.max(0, exp.toInstant().getEpochSecond() - Instant.now().getEpochSecond());
        return Duration.ofSeconds(seconds);
    }
}
