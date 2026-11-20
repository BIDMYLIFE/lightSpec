package com.lightspec.auth.service;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Service
public class AuthCookieService {

    public static final String COOKIE_NAME = "LIGHTSPEC_AUTH";
    private final Duration ttl;
    private final boolean secure;

    public AuthCookieService(JwtService jwtService) {
        this(jwtService, true);
    }

    @Autowired
    public AuthCookieService(
            JwtService jwtService,
            @Value("${lightspec.security.cookie-secure:true}") boolean secure) {
        this.ttl = jwtService.ttl();
        this.secure = secure;
    }

    public ResponseCookie create(String token) {
        return cookie(token, ttl);
    }

    public ResponseCookie clear() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
