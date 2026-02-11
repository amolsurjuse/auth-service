package com.electrahub.identity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceApplicationTest {

    @Test
    void applicationHasSpringBootAnnotation() {
        SpringBootApplication annotation = AuthServiceApplication.class.getAnnotation(SpringBootApplication.class);
        assertThat(annotation).isNotNull();
    }

    @Test
    void applicationClassConstructs() {
        AuthServiceApplication app = new AuthServiceApplication();
        assertThat(app).isNotNull();
    }
}
