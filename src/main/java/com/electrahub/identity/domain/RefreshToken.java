package com.electrahub.identity.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected RefreshToken() {}

    public RefreshToken(UUID id, User user, String deviceId, String tokenHash,
                        OffsetDateTime expiresAt, OffsetDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.deviceId = deviceId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = false;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getDeviceId() { return deviceId; }
    public String getTokenHash() { return tokenHash; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }

    public void revoke() { this.revoked = true; }

    public boolean isExpiredNow() {
        return expiresAt.isBefore(OffsetDateTime.now());
    }
}

