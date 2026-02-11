package com.electrahub.identity.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class JwtService {

    private final Key signingKey;
    private final String issuer;
    private final long accessTtlMinutes;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.access-token-ttl-minutes}") long accessTtlMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public record ParsedToken(String subjectEmail, String jti, String uid, long tv, Date exp) {}

    public String generateAccessToken(String subjectEmail, String uid, long tokenVersion, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTtlMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(subjectEmail)
                .issuer(issuer)
                .id(UUID.randomUUID().toString()) // jti
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("uid", uid)
                .claim("tv", tokenVersion)
                .claim("roles", roles)
                .signWith(signingKey)
                .compact();
    }

    public ParsedToken parseAndValidate(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token);

        Claims c = jws.getPayload();

        if (!issuer.equals(c.getIssuer())) {
            throw new JwtException("Invalid issuer");
        }

        Object tvObj = c.getOrDefault("tv", 0);
        long tv = (tvObj instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(tvObj));

        return new ParsedToken(
                c.getSubject(),
                c.getId(),
                String.valueOf(c.get("uid")),
                tv,
                c.getExpiration()
        );
    }

    public boolean isNotExpired(Date exp) {
        return exp != null && exp.after(new Date());
    }
}

