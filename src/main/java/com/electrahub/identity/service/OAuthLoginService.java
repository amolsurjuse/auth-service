package com.electrahub.identity.service;

import com.electrahub.identity.domain.OAuthIdentity;
import com.electrahub.identity.exception.ConflictException;
import com.electrahub.identity.integration.UserServiceClient;
import com.electrahub.identity.repository.OAuthIdentityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OAuthLoginService {

    private static final String GOOGLE_PROVIDER = "GOOGLE";
    private static final String FACEBOOK_PROVIDER = "FACEBOOK";

    private final GoogleOidcTokenVerifier googleVerifier;
    private final FacebookOAuthTokenVerifier facebookVerifier;
    private final OAuthIdentityRepository identityRepository;
    private final UserServiceClient userServiceClient;
    private final AuthService authService;
    private final boolean googleAutoProvisionUsers;
    private final boolean facebookAutoProvisionUsers;

    public OAuthLoginService(
            GoogleOidcTokenVerifier googleVerifier,
            FacebookOAuthTokenVerifier facebookVerifier,
            OAuthIdentityRepository identityRepository,
            UserServiceClient userServiceClient,
            AuthService authService,
            @Value("${app.security.oauth.google.auto-provision-users:true}") boolean googleAutoProvisionUsers,
            @Value("${app.security.oauth.facebook.auto-provision-users:true}") boolean facebookAutoProvisionUsers
    ) {
        this.googleVerifier = googleVerifier;
        this.facebookVerifier = facebookVerifier;
        this.identityRepository = identityRepository;
        this.userServiceClient = userServiceClient;
        this.authService = authService;
        this.googleAutoProvisionUsers = googleAutoProvisionUsers;
        this.facebookAutoProvisionUsers = facebookAutoProvisionUsers;
    }

    @Transactional
    public AuthService.TokenPair loginWithGoogle(String idToken, String nonce, String deviceId) {
        return loginWithGoogle(idToken, nonce, deviceId, null);
    }

    @Transactional
    public AuthService.TokenPair loginWithGoogle(String idToken, String nonce, String deviceId, Duration refreshTtl) {
        GoogleOidcPrincipal googlePrincipal = googleVerifier.verify(idToken, nonce);
        OAuthIdentity identity = identityRepository
                .findByProviderAndProviderSubject(GOOGLE_PROVIDER, googlePrincipal.subject())
                .map(existing -> updateExistingIdentity(existing, googlePrincipal))
                .orElseGet(() -> createIdentity(googlePrincipal));

        UserServiceClient.UserPrincipal principal = userServiceClient.getPrincipal(identity.getUserId());
        if (googlePrincipal.emailVerified() && !principal.isEmailVerified()) {
            principal = userServiceClient.markEmailVerified(principal.userId());
        }
        return refreshTtl == null
                ? authService.issueTokensForPrincipal(principal, deviceId)
                : authService.issueTokensForPrincipal(principal, deviceId, refreshTtl);
    }

    @Transactional
    public AuthService.TokenPair loginWithFacebook(String accessToken, String deviceId) {
        return loginWithFacebook(accessToken, deviceId, null);
    }

    @Transactional
    public AuthService.TokenPair loginWithFacebook(String accessToken, String deviceId, Duration refreshTtl) {
        FacebookOAuthPrincipal facebookPrincipal = facebookVerifier.verify(accessToken);
        OAuthIdentity identity = identityRepository
                .findByProviderAndProviderSubject(FACEBOOK_PROVIDER, facebookPrincipal.subject())
                .map(existing -> updateExistingIdentity(existing, facebookPrincipal))
                .orElseGet(() -> createIdentity(facebookPrincipal));

        UserServiceClient.UserPrincipal principal = userServiceClient.getPrincipal(identity.getUserId());
        if (facebookPrincipal.emailVerified() && !principal.isEmailVerified()) {
            principal = userServiceClient.markEmailVerified(principal.userId());
        }
        return refreshTtl == null
                ? authService.issueTokensForPrincipal(principal, deviceId)
                : authService.issueTokensForPrincipal(principal, deviceId, refreshTtl);
    }

    private OAuthIdentity updateExistingIdentity(OAuthIdentity identity, GoogleOidcPrincipal principal) {
        identity.markLogin(
                OffsetDateTime.now(),
                principal.email(),
                principal.emailVerified(),
                principal.givenName(),
                principal.familyName(),
                principal.pictureUrl()
        );
        return identityRepository.save(identity);
    }

    private OAuthIdentity createIdentity(GoogleOidcPrincipal principal) {
        if (!googleAutoProvisionUsers) {
            throw new ConflictException("Google account is not linked to an ElectraHub account");
        }

        UserServiceClient.UserPrincipal userPrincipal = registerNewUser(principal);
        OffsetDateTime now = OffsetDateTime.now();
        OAuthIdentity identity = new OAuthIdentity(
                UUID.randomUUID(),
                GOOGLE_PROVIDER,
                principal.subject(),
                userPrincipal.userId(),
                principal.email(),
                principal.emailVerified(),
                principal.givenName(),
                principal.familyName(),
                principal.pictureUrl(),
                now,
                now
        );

        try {
            return identityRepository.save(identity);
        } catch (DataIntegrityViolationException ex) {
            return identityRepository.findByProviderAndProviderSubject(GOOGLE_PROVIDER, principal.subject())
                    .orElseThrow(() -> ex);
        }
    }

    private OAuthIdentity updateExistingIdentity(OAuthIdentity identity, FacebookOAuthPrincipal principal) {
        identity.markLogin(
                OffsetDateTime.now(),
                principal.email(),
                principal.emailVerified(),
                principal.givenName(),
                principal.familyName(),
                principal.pictureUrl()
        );
        return identityRepository.save(identity);
    }

    private OAuthIdentity createIdentity(FacebookOAuthPrincipal principal) {
        if (!facebookAutoProvisionUsers) {
            throw new ConflictException("Facebook account is not linked to an ElectraHub account");
        }

        UserServiceClient.UserPrincipal userPrincipal = registerNewUser(
                principal.email(),
                principal.givenName(),
                principal.familyName()
        );
        OffsetDateTime now = OffsetDateTime.now();
        OAuthIdentity identity = new OAuthIdentity(
                UUID.randomUUID(),
                FACEBOOK_PROVIDER,
                principal.subject(),
                userPrincipal.userId(),
                principal.email(),
                principal.emailVerified(),
                principal.givenName(),
                principal.familyName(),
                principal.pictureUrl(),
                now,
                now
        );

        try {
            return identityRepository.save(identity);
        } catch (DataIntegrityViolationException ex) {
            return identityRepository.findByProviderAndProviderSubject(FACEBOOK_PROVIDER, principal.subject())
                    .orElseThrow(() -> ex);
        }
    }

    private UserServiceClient.UserPrincipal registerNewUser(GoogleOidcPrincipal principal) {
        try {
            return registerNewUser(principal.email(), principal.givenName(), principal.familyName());
        } catch (ConflictException ex) {
            // Google has already verified ownership of this email. Link the Google
            // subject to the existing ElectraHub account instead of requiring the
            // user to sign in with a password before social login can succeed.
            return userServiceClient.getPrincipalByEmail(principal.email());
        }
    }

    private UserServiceClient.UserPrincipal registerNewUser(String email, String givenName, String familyName) {
        try {
            UserServiceClient.UserPrincipal userPrincipal = userServiceClient.register(new UserServiceClient.RegisterUserRequest(
                    email,
                    generatedExternalAccountPassword(),
                    givenName,
                    familyName,
                    null,
                    null
            ));
            if (userPrincipal == null) {
                throw new IllegalStateException("User service returned an empty principal");
            }
            return userPrincipal;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new ConflictException("Email already registered; sign in with password before linking this social account");
            }
            throw ex;
        }
    }

    private String generatedExternalAccountPassword() {
        return UUID.randomUUID() + "Aa1!";
    }
}
