package com.electrahub.identity.exception;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.identity.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandlerTest.class);


    /**
     * Processes handle validation uses first error message for `GlobalExceptionHandlerTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.exception`.
     */
    @Test
    void handleValidationUsesFirstErrorMessage() throws Exception {
        LOGGER.info("CODEx_ENTRY_LOG: Entering GlobalExceptionHandlerTest#handleValidationUsesFirstErrorMessage");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering GlobalExceptionHandlerTest#handleValidationUsesFirstErrorMessage with debug context");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        LoginRequest reqObj = new LoginRequest("bad", "");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(reqObj, "req");
        bindingResult.addError(new FieldError("req", "email", "Invalid email"));

        Method method = TestController.class.getDeclaredMethod("login", LoginRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/login");

        ResponseEntity<ApiError> response = handler.handleValidation(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid email");
        assertThat(response.getBody().path()).isEqualTo("/api/auth/login");
    }

    /**
     * Processes handle bad request builds error for `GlobalExceptionHandlerTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.exception`.
     */
    @Test
    void handleBadRequestBuildsError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/register");

        ResponseEntity<ApiError> response = handler.handleBadRequest(new IllegalArgumentException("bad"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("bad");
        assertThat(response.getBody().path()).isEqualTo("/api/auth/register");
    }

    /**
     * Processes handle generic uses unexpected message for `GlobalExceptionHandlerTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.exception`.
     */
    @Test
    void handleGenericUsesUnexpectedMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = new MockHttpServletRequest();
        ((MockHttpServletRequest) req).setRequestURI("/api/other");

        ResponseEntity<ApiError> response = handler.handleGeneric(new RuntimeException("boom"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
        assertThat(response.getBody().path()).isEqualTo("/api/other");
    }

    @Test
    void handleDisabledReturnsForbiddenWithOriginalMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/login");

        ResponseEntity<ApiError> response = handler.handleDisabled(new DisabledException("User account is pending deletion"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("User account is pending deletion");
        assertThat(response.getBody().path()).isEqualTo("/api/auth/login");
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void login(LoginRequest req) {}
    }
}
