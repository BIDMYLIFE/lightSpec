package com.lightspec.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTests {

    private final JwtService jwtService = new JwtService(
            "cookie-test-secret-with-at-least-thirty-two-bytes", Duration.ofMinutes(15), Clock.systemUTC());
    private final AuthCookieService cookies = new AuthCookieService(jwtService);

    @Test
    void createsSecureHttpOnlyStrictCookieAndCanClearIt() {
        assertThat(cookies.create("signed-token").toString())
                .contains("LIGHTSPEC_AUTH=signed-token")
                .contains("Path=/")
                .contains("Max-Age=900")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Strict");

        assertThat(cookies.clear().toString())
                .contains("LIGHTSPEC_AUTH=")
                .contains("Max-Age=0")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Strict");
    }
}
