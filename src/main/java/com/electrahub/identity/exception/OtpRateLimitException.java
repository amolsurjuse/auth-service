package com.electrahub.identity.exception;

public class OtpRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public OtpRateLimitException(long retryAfterSeconds) {
        super("Please wait before requesting another verification code.");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
