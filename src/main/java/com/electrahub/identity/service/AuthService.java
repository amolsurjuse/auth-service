package com.electrahub.identity.service;

import com.electrahub.identity.domain.*;
import com.electrahub.identity.repository.AddressRepository;
import com.electrahub.identity.repository.CountryRepository;
import com.electrahub.identity.repository.RefreshTokenRepository;
import com.electrahub.identity.repository.RoleRepository;
import com.electrahub.identity.repository.UserRepository;
import com.electrahub.identity.web.dto.AddressDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AddressRepository addressRepository;
    private final CountryRepository countryRepository;

    private final RedisRefreshSessionStore refreshStore;
    private final TokenVersionService tokenVersionService;

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    private final long refreshTtlDays;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            AddressRepository addressRepository,
            CountryRepository countryRepository,
            RedisRefreshSessionStore refreshStore,
            TokenVersionService tokenVersionService,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.jwt.refresh-token-ttl-days}") long refreshTtlDays
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.addressRepository = addressRepository;
        this.countryRepository = countryRepository;
        this.refreshStore = refreshStore;
        this.tokenVersionService = tokenVersionService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.refreshTtlDays = refreshTtlDays;
    }

    public record TokenPair(String accessToken, String refreshToken) {}

    @Transactional
    public TokenPair register(String email, String rawPassword, String deviceId) {
        String normalized = email.toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            throw new IllegalArgumentException("Email already registered");
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = new User(UUID.randomUUID(), normalized, passwordEncoder.encode(rawPassword), true, now);

        // Assign default USER role (seeded by Liquibase)
        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER not seeded"));
        user.addRole(userRole);

        userRepository.save(user);
        return issueTokens(user, deviceId);
    }

    @Transactional
    public TokenPair register(String email, String rawPassword, String deviceId,
                              String firstName, String lastName, String phoneNumber, AddressDto addressDto) {
        String normalized = email.toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            throw new IllegalArgumentException("Email already registered");
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = new User(UUID.randomUUID(), normalized, passwordEncoder.encode(rawPassword), true, now);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);

        if (addressDto != null) {
            Address address = buildAddress(addressDto);
            if (address != null) {
                addressRepository.save(address);
                user.setAddress(address);
            }
        }

        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER not seeded"));
        user.addRole(userRole);

        userRepository.save(user);
        return issueTokens(user, deviceId);
    }

    @Transactional
    public TokenPair login(String email, String rawPassword, String deviceId) {
        String normalized = email.toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalized, rawPassword));

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return issueTokens(user, deviceId);
    }

    @Transactional
    public TokenPair refresh(String refreshPlain, String deviceId) {
        String hash = sha256Hex(refreshPlain);

        // Fast-path: Redis view (device binding)
        var view = refreshStore.getIfPresent(hash);

        RefreshToken db = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (db.isRevoked() || db.isExpiredNow()) {
            // cleanup best-effort
            refreshStore.delete(hash, db.getUser().getId(), db.getDeviceId());
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        // Device binding check
        String expectedDevice = db.getDeviceId();
        if (!expectedDevice.equals(deviceId)) {
            throw new IllegalArgumentException("Refresh token device mismatch");
        }
        if (view != null && !view.deviceId().equals(deviceId)) {
            throw new IllegalArgumentException("Refresh token device mismatch");
        }

        // Rotate: revoke old
        db.revoke();
        refreshStore.delete(hash, db.getUser().getId(), deviceId);

        return issueTokens(db.getUser(), deviceId);
    }

    @Transactional
    public void revokeRefreshForUserDevice(UUID userId, String deviceId) {
        // Immediate enforcement in Redis
        refreshStore.revokeAllForUserDevice(userId, deviceId);
        // Durable cleanup
        refreshTokenRepository.deleteByUser_IdAndDeviceId(userId, deviceId);
    }

    @Transactional
    public void revokeAllRefreshForUser(UUID userId) {
        refreshStore.revokeAllForUser(userId);
        refreshTokenRepository.deleteByUser_Id(userId);
    }

    private TokenPair issueTokens(User user, String deviceId) {
        long tv = tokenVersionService.getVersion(user.getId());
        var roles = user.getRoles().stream().map(r -> r.getName()).toList();

        String access = jwtService.generateAccessToken(user.getEmail(), user.getId().toString(), tv, roles);

        String refreshPlain = UUID.randomUUID() + "." + UUID.randomUUID();
        String refreshHash = sha256Hex(refreshPlain);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime exp = now.plusDays(refreshTtlDays);
        Duration ttl = Duration.ofDays(refreshTtlDays);

        RefreshToken rt = new RefreshToken(UUID.randomUUID(), user, deviceId, refreshHash, exp, now);
        refreshTokenRepository.save(rt);

        refreshStore.put(
                refreshHash,
                new RedisRefreshSessionStore.RefreshSessionView(user.getId(), deviceId, rt.getId(), exp),
                ttl
        );

        return new TokenPair(access, refreshPlain);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash", e);
        }
    }

    private Address buildAddress(AddressDto dto) {
        if (dto == null) return null;

        Country country = null;
        if (dto.countryIsoCode() != null && !dto.countryIsoCode().isBlank()) {
            country = countryRepository.findByIsoCodeAndEnabledTrue(dto.countryIsoCode().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Country not available"));
        }

        return new Address(
                UUID.randomUUID(),
                dto.street(),
                dto.city(),
                dto.state(),
                dto.postalCode(),
                country
        );
    }
}
