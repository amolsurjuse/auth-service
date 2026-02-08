package com.amy.auth.exception;

import com.amy.auth.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handleValidationUsesFirstErrorMessage() throws Exception {
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

    private static class TestController {
        @SuppressWarnings("unused")
        void login(LoginRequest req) {}
    }
}
