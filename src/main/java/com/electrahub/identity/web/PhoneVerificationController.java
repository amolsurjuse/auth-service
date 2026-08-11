package com.electrahub.identity.web;

import com.electrahub.identity.service.PhoneVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/phone-verification")
public class PhoneVerificationController {
    private final PhoneVerificationService service;

    public PhoneVerificationController(PhoneVerificationService service) { this.service = service; }

    @PostMapping("/request")
    ResponseEntity<PhoneVerificationService.ChallengeStatus> request(HttpServletRequest request) {
        return ResponseEntity.accepted().body(service.request(userId(request)));
    }

    @PostMapping("/verify")
    ResponseEntity<AuthController.AcceptedResponse> verify(
            @Valid @RequestBody VerifyPhoneOtpRequest body, HttpServletRequest request) {
        service.verify(userId(request), body.challengeId(), body.code());
        return ResponseEntity.ok(new AuthController.AcceptedResponse("OK", "Phone number has been verified."));
    }

    private static UUID userId(HttpServletRequest request) {
        Object value = request.getAttribute("uid");
        if (value == null) throw new AuthenticationCredentialsNotFoundException("Authentication required");
        try { return UUID.fromString(String.valueOf(value)); }
        catch (IllegalArgumentException exception) { throw new AuthenticationCredentialsNotFoundException("Authentication required"); }
    }

    record VerifyPhoneOtpRequest(@NotNull UUID challengeId,
            @NotBlank @Pattern(regexp="\\d{6}") String code) {}
}
