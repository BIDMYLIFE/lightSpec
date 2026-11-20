package com.lightspec.auth.web;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserEntity;
import com.lightspec.auth.repository.UserRepository;
import com.lightspec.auth.service.AuthCookieService;
import com.lightspec.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SecurityIntegrationTests.SecurityTestController.class)
class SecurityIntegrationTests {

    private static final Path DATABASE = Path.of("target", "security-" + UUID.randomUUID() + ".db")
            .toAbsolutePath();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @LocalServerPort
    int port;

    @Autowired
    UserRepository users;

    @Autowired
    JwtService jwtService;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void cleanUsers() {
        users.deleteAll();
    }

    @Test
    void publicEndpointDoesNotRequireAuthentication() throws Exception {
        assertThat(get("/api/setup/status", null).statusCode()).isEqualTo(200);
    }

    @Test
    void protectedEndpointRejectsMissingAndInvalidJwt() throws Exception {
        assertThat(get("/api/auth/me", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/auth/me", "not-a-jwt").statusCode()).isEqualTo(401);
    }

    @Test
    void validJwtAuthenticatesAndRoleRulesRejectInsufficientAuthority() throws Exception {
        var user = users.saveAndFlush(new UserEntity("member", "member", "hash", Role.USER));
        var admin = users.saveAndFlush(new UserEntity("admin", "admin", "hash", Role.ADMIN));

        assertThat(get("/api/test/authenticated", jwtService.issue(user)).statusCode()).isEqualTo(204);
        assertThat(get("/api/admin/test", jwtService.issue(user)).statusCode()).isEqualTo(403);
        assertThat(get("/api/admin/test", jwtService.issue(admin)).statusCode()).isEqualTo(204);
    }

    @Test
    void stateChangingPublicEndpointStillRequiresCsrf() throws Exception {
        var admin = users.saveAndFlush(new UserEntity("admin", "admin", "hash", Role.ADMIN));
        var request = HttpRequest.newBuilder(uri("/api/auth/register"))
                .header("Content-Type", "application/json")
                .header("Cookie", AuthCookieService.COOKIE_NAME + "=" + jwtService.issue(admin))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"member\",\"password\":\"safe-password-123\",\"passwordConfirmation\":\"safe-password-123\"}"))
                .build();

        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        var builder = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) {
            builder.header("Cookie", AuthCookieService.COOKIE_NAME + "=" + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @RestController
    static class SecurityTestController {

        @GetMapping({"/api/test/authenticated", "/api/admin/test"})
        ResponseEntity<Void> authenticated() {
            return ResponseEntity.noContent().build();
        }
    }
}
