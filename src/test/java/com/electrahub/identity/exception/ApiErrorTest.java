package com.electrahub.identity.exception;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiErrorTest.class);


    /**
     * Executes record holds fields for `ApiErrorTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.exception`.
     */
    @Test
    void recordHoldsFields() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering ApiErrorTest#recordHoldsFields");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering ApiErrorTest#recordHoldsFields with debug context");
        OffsetDateTime ts = OffsetDateTime.now();
        ApiError error = new ApiError(ts, 400, "Bad Request", "msg", "/path");

        assertThat(error.timestamp()).isEqualTo(ts);
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.error()).isEqualTo("Bad Request");
        assertThat(error.message()).isEqualTo("msg");
        assertThat(error.path()).isEqualTo("/path");
    }
}
