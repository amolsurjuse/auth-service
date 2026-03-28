package com.electrahub.identity.grpc;

import com.electrahub.proto.identity.v1.AuthServiceGrpc;
import com.electrahub.proto.identity.v1.RegisterRequest;
import com.electrahub.proto.identity.v1.LoginRequest;
import com.electrahub.proto.identity.v1.RefreshRequest;
import com.electrahub.proto.identity.v1.LogoutDeviceRequest;
import com.electrahub.proto.identity.v1.LogoutAllRequest;
import com.electrahub.proto.identity.v1.TokenResponse;
import com.electrahub.identity.service.AuthService;
import com.electrahub.identity.service.TokenDenylistService;
import com.electrahub.identity.service.TokenVersionService;
import com.electrahub.identity.web.dto.AddressDto;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.UUID;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthGrpcService.class);

    private final AuthService authService;
    private final TokenDenylistService tokenDenylistService;
    private final TokenVersionService tokenVersionService;

    public AuthGrpcService(
            AuthService authService,
            TokenDenylistService tokenDenylistService,
            TokenVersionService tokenVersionService
    ) {
        this.authService = authService;
        this.tokenDenylistService = tokenDenylistService;
        this.tokenVersionService = tokenVersionService;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<TokenResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Registering user with email: {}", request.getEmail());

            AddressDto addressDto = null;
            if (request.hasAddress()) {
                addressDto = new AddressDto(
                        request.getAddress().getStreet(),
                        request.getAddress().getCity(),
                        request.getAddress().getState(),
                        request.getAddress().getPostalCode(),
                        request.getAddress().getCountry()
                );
            }

            AuthService.TokenPair pair = authService.register(
                    request.getEmail(),
                    request.getPassword(),
                    request.getDeviceId(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhoneNumber(),
                    addressDto
            );

            TokenResponse response = TokenResponse.newBuilder()
                    .setAccessToken(pair.accessToken())
                    .setRefreshToken(pair.refreshToken())
                    .setTokenType("Bearer")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in register", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in register", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void login(LoginRequest request, StreamObserver<TokenResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Login attempt for email: {}", request.getEmail());

            AuthService.TokenPair pair = authService.login(
                    request.getEmail(),
                    request.getPassword(),
                    request.getDeviceId()
            );

            TokenResponse response = TokenResponse.newBuilder()
                    .setAccessToken(pair.accessToken())
                    .setRefreshToken(pair.refreshToken())
                    .setTokenType("Bearer")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (BadCredentialsException e) {
            LOGGER.warn("Bad credentials in login");
            responseObserver.onError(
                    Status.UNAUTHENTICATED
                            .withDescription("Invalid credentials")
                            .asException()
            );
        } catch (DisabledException e) {
            LOGGER.warn("User disabled in login");
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription("User is disabled")
                            .asException()
            );
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in login", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in login", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void refresh(RefreshRequest request, StreamObserver<TokenResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Refreshing token for device: {}", request.getDeviceId());

            AuthService.TokenPair pair = authService.refresh(
                    request.getRefreshToken(),
                    request.getDeviceId()
            );

            TokenResponse response = TokenResponse.newBuilder()
                    .setAccessToken(pair.accessToken())
                    .setRefreshToken(pair.refreshToken())
                    .setTokenType("Bearer")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid refresh token");
            responseObserver.onError(
                    Status.UNAUTHENTICATED
                            .withDescription("Invalid or expired refresh token")
                            .asException()
            );
        } catch (DisabledException e) {
            LOGGER.warn("User disabled during refresh");
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription("User is disabled")
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in refresh", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void logoutDevice(LogoutDeviceRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            LOGGER.debug("gRPC: Logout device for user: {}, device: {}", request.getUserId(), request.getDeviceId());

            authService.revokeRefreshForUserDevice(UUID.fromString(request.getUserId()), request.getDeviceId());

            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in logoutDevice", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in logoutDevice", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void logoutAll(LogoutAllRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            LOGGER.debug("gRPC: Logout all devices for user: {}", request.getUserId());

            UUID userId = UUID.fromString(request.getUserId());
            authService.revokeAllRefreshForUser(userId);
            tokenVersionService.bumpVersion(userId);

            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in logoutAll", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in logoutAll", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }
}
