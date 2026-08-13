package com.electrahub.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth_identities")
public class OAuthIdentity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "given_name", length = 120)
    private String givenName;

    @Column(name = "family_name", length = 120)
    private String familyName;

    @Column(name = "picture_url", length = 1024)
    private String pictureUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_login_at", nullable = false)
    private OffsetDateTime lastLoginAt;

    protected OAuthIdentity() {
    }

    public OAuthIdentity(UUID id,
                         String provider,
                         String providerSubject,
                         UUID userId,
                         String email,
                         boolean emailVerified,
                         String givenName,
                         String familyName,
                         String pictureUrl,
                         OffsetDateTime createdAt,
                         OffsetDateTime lastLoginAt) {
        this.id = id;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.userId = userId;
        this.email = email;
        this.emailVerified = emailVerified;
        this.givenName = givenName;
        this.familyName = familyName;
        this.pictureUrl = pictureUrl;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void markLogin(OffsetDateTime at,
                          String email,
                          boolean emailVerified,
                          String givenName,
                          String familyName,
                          String pictureUrl) {
        this.lastLoginAt = at;
        this.email = email;
        this.emailVerified = emailVerified;
        this.givenName = givenName;
        this.familyName = familyName;
        this.pictureUrl = pictureUrl;
    }
}
