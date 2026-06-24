package com.electrahub.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendEmailVerificationRequest(@Email @NotBlank String email) {
}
