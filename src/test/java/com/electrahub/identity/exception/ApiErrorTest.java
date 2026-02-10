package com.electrahub.identity.exception;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {

    @Test
    void recordHoldsFields() {
        OffsetDateTime ts = OffsetDateTime.now();
        ApiError error = new ApiError(ts, 400, "Bad Request", "msg", "/path");

        assertThat(error.timestamp()).isEqualTo(ts);
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.error()).isEqualTo("Bad Request");
        assertThat(error.message()).isEqualTo("msg");
        assertThat(error.path()).isEqualTo("/path");
    }
}
