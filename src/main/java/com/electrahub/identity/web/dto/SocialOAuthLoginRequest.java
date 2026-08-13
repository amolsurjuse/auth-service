package com.electrahub.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialOAuthLoginRequest(@NotBlank String accessToken) {
}
