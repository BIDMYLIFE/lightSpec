package com.lightspec.auth;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserRepository;
import com.lightspec.auth.service.AuthCookieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationEndToEndTests {

    private static final Path DATABASE = Path.of("target", "e2e-" + UUID.randomUUID() + ".db").toAbsolutePath();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository users;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void completesTheEntireAuthenticationJourneyFromAnEmptyDatabase() throws Exception {
        assertThat(get("/api/setup/status", null).body()).isEqualTo("{\"setupRequired\":true}");

        var admin = post("/api/setup/admin", credentials("First-Admin", "admin-password-123"), null);
        assertThat(admin.statusCode()).isEqualTo(201);
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(get("/api/setup/status", null).body()).isEqualTo("{\"setupRequired\":false}");

        var repeatedSetup = post("/api/setup/admin", credentials("Second-Admin", "admin-password-456"), null);
        assertThat(repeatedSetup.statusCode()).isEqualTo(409);
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);

        var registration = post("/api/auth/register", credentials("Display-User", "user-password-123"), null);
        assertThat(registration.statusCode()).isEqualTo(201);
        assertThat(users.countByRole(Role.USER)).isEqualTo(1);

        var loginBody = objectMapper.createObjectNode()
                .put("username", "display-user")
                .put("password", "user-password-123")
                .toString();
        var login = post("/api/auth/login", loginBody, null);
        assertThat(login.statusCode()).isEqualTo(200);
        var authToken = authToken(login);

        var me = get("/api/auth/me", authToken);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("\"username\":\"Display-User\"");

        var logout = post("/api/auth/logout", "", authToken);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(logout.headers().allValues(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith(AuthCookieService.COOKIE_NAME + "="))
                .findFirst()).hasValueSatisfying(value -> assertThat(value).contains("Max-Age=0"));
        assertThat(get("/api/auth/me", null).statusCode()).isEqualTo(401);
    }

    private String credentials(String username, String password) throws Exception {
        return objectMapper.createObjectNode()
                .put("username", username)
                .put("password", password)
                .put("passwordConfirmation", password)
                .toString();
    }

    private HttpResponse<String> post(String path, String body, String authToken) throws Exception {
        var csrfResponse = get("/api/security/csrf", null);
        var csrf = objectMapper.readTree(csrfResponse.body());
        var csrfCookie = csrfResponse.headers().allValues(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst().orElseThrow();
        var cookies = csrfCookie.substring(0, csrfCookie.indexOf(';'));
        if (authToken != null) cookies += "; " + AuthCookieService.COOKIE_NAME + "=" + authToken;

        var request = HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.COOKIE, cookies)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String authToken) throws Exception {
        var builder = HttpRequest.newBuilder(uri(path)).GET();
        if (authToken != null) builder.header(HttpHeaders.COOKIE, AuthCookieService.COOKIE_NAME + "=" + authToken);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String authToken(HttpResponse<String> response) {
        var setCookie = response.headers().allValues(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith(AuthCookieService.COOKIE_NAME + "="))
                .findFirst().orElseThrow();
        var pair = setCookie.substring(0, setCookie.indexOf(';'));
        return pair.substring(pair.indexOf('=') + 1);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
