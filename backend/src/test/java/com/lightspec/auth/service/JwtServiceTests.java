package com.lightspec.auth.service;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtException;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private static final String SECRET = "unit-test-secret-with-at-least-thirty-two-bytes";
    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");

    @Test
    void issuesShortLivedTokenWithIdentityClaims() throws Exception {
        var service = service(SECRET, NOW, Duration.ofMinutes(15));
        var user = user(42, "測試使用者", Role.USER);

        var jwt = service.decode(service.issue(user));

        assertThat(jwt.getIssuer()).hasToString("https://lightspec.local");
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("測試使用者");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(jwt.getId()).isNotBlank();
    }

    @Test
    void rejectsExpiredTamperedAndWronglySignedTokens() throws Exception {
        var expiredIssuer = service(SECRET, Instant.now().minus(Duration.ofHours(2)), Duration.ofMinutes(5));
        var currentIssuer = service(SECRET, Instant.now(), Duration.ofMinutes(5));
        var token = currentIssuer.issue(user(7, "member", Role.USER));

        assertThatThrownBy(() -> currentIssuer.decode(expiredIssuer.issue(user(7, "member", Role.USER))))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> currentIssuer.decode(tamperSignature(token)))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service("different-secret-with-at-least-thirty-two-bytes", Instant.now(), Duration.ofMinutes(5))
                .decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void refusesToStartWithoutAStrongEnvironmentSecret() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtConfiguration.class)
                .run(context -> assertThat(context).hasFailed());

        new ApplicationContextRunner()
                .withUserConfiguration(JwtConfiguration.class)
                .withPropertyValues("lightspec.security.jwt.secret=too-short")
                .run(context -> assertThat(context).hasFailed());
    }

    private JwtService service(String secret, Instant instant, Duration ttl) {
        return new JwtService(secret, ttl, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private UserEntity user(int id, String username, Role role) throws Exception {
        var user = new UserEntity(username, username.toLowerCase(), "hash", role);
        Field idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    private String tamperSignature(String token) {
        var parts = token.split("\\.");
        var signature = parts[2];
        parts[2] = (signature.charAt(0) == 'A' ? "B" : "A") + signature.substring(1);
        return String.join(".", parts);
    }
}
