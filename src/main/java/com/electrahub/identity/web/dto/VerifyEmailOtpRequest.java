package com.electrahub.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyEmailOtpRequest(
        @NotNull UUID challengeId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Verification code must contain six digits") String code
) {
}
