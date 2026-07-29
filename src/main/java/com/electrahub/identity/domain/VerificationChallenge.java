package com.electrahub.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_challenges")
public class VerificationChallenge {
    public enum Status { ACTIVE, VERIFIED, SUPERSEDED, LOCKED }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String purpose;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(name = "destination_hash", nullable = false, length = 64)
    private String destinationHash;

    @Column(name = "masked_destination", nullable = false, length = 320)
    private String maskedDestination;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected VerificationChallenge() {
    }

    public VerificationChallenge(
            UUID id,
            UUID userId,
            String purpose,
            String channel,
            String destinationHash,
            String maskedDestination,
            String codeHash,
            int maxAttempts,
            OffsetDateTime expiresAt,
            OffsetDateTime now
    ) {
        this.id = id;
        this.userId = userId;
        this.purpose = purpose;
        this.channel = channel;
        this.destinationHash = destinationHash;
        this.maskedDestination = maskedDestination;
        this.codeHash = codeHash;
        this.status = Status.ACTIVE;
        this.maxAttempts = maxAttempts;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPurpose() { return purpose; }
    public String getChannel() { return channel; }
    public String getMaskedDestination() { return maskedDestination; }
    public String getCodeHash() { return codeHash; }
    public Status getStatus() { return status; }
    public int getFailedAttempts() { return failedAttempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public int attemptsRemaining() {
        return Math.max(0, maxAttempts - failedAttempts);
    }

    public void recordFailure(OffsetDateTime now) {
        failedAttempts++;
        updatedAt = now;
        if (failedAttempts >= maxAttempts) {
            status = Status.LOCKED;
        }
    }

    public void verify(OffsetDateTime now) {
        status = Status.VERIFIED;
        consumedAt = now;
        updatedAt = now;
    }

    public void supersede(OffsetDateTime now) {
        status = Status.SUPERSEDED;
        updatedAt = now;
    }
}
