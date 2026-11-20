package com.lightspec.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class JwtConfiguration {

    @Bean
    Clock jwtClock() {
        return Clock.systemUTC();
    }

    @Bean
    JwtService jwtService(
            @Value("${lightspec.security.jwt.secret:}") String secret,
            @Value("${lightspec.security.jwt.ttl:PT15M}") String ttl,
            Clock jwtClock) {
        return new JwtService(secret, Duration.parse(ttl), jwtClock);
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService.decoder();
    }
}
