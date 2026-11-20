package com.lightspec.auth.web.api;

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
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationApiIntegrationTests {

    private static final String TEST_SECRET = "test-only-secret-with-at-least-thirty-two-bytes";
    private static final Path DATABASE = Path.of("target", "authentication-" + UUID.randomUUID() + ".db")
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
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void cleanAndInitialize() {
        users.deleteAll();
        users.saveAndFlush(new UserEntity("admin", "admin", passwordEncoder.encode("admin-password"), Role.ADMIN));
    }

    @Test
    void loginSetsSecureCookieAndMeReturnsDisplayName() throws Exception {
        users.saveAndFlush(new UserEntity("Display-Name", "display-name",
                passwordEncoder.encode("safe-password-123"), Role.USER));

        var login = postWithCsrf("/api/auth/login", loginBody("DISPLAY-NAME", "safe-password-123"), null);

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("\"username\":\"Display-Name\"").contains("\"role\":\"USER\"");
        var setCookie = authSetCookie(login);
        assertThat(setCookie).contains("Secure").contains("HttpOnly").contains("SameSite=Strict");

        var me = get("/api/auth/me", cookieValue(setCookie));
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("\"username\":\"Display-Name\"");
    }

    @Test
    void wrongUsernameWrongPasswordAndDisabledAccountShareOneError() throws Exception {
        var disabled = users.saveAndFlush(new UserEntity("disabled", "disabled",
                passwordEncoder.encode("safe-password-123"), Role.USER));
        disabled.disable();
        users.saveAndFlush(disabled);

        var unknown = postWithCsrf("/api/auth/login", loginBody("unknown", "safe-password-123"), null);
        var wrongPassword = postWithCsrf("/api/auth/login", loginBody("disabled", "wrong-password"), null);
        var disabledLogin = postWithCsrf("/api/auth/login", loginBody("disabled", "safe-password-123"), null);

        assertThat(unknown.statusCode()).isEqualTo(401);
        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(disabledLogin.statusCode()).isEqualTo(401);
        assertThat(unknown.body()).isEqualTo(wrongPassword.body()).isEqualTo(disabledLogin.body());
        assertThat(unknown.body()).contains("urn:lightspec:problem:invalid-login");
    }

    @Test
    void logoutClearsCookieAndExpiredSessionIsRejected() throws Exception {
        var user = users.saveAndFlush(new UserEntity("member", "member",
                passwordEncoder.encode("safe-password-123"), Role.USER));
        var login = postWithCsrf("/api/auth/login", loginBody("member", "safe-password-123"), null);
        var authCookie = cookieValue(authSetCookie(login));

        var logout = postWithCsrf("/api/auth/logout", "", authCookie);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(authSetCookie(logout)).contains("Max-Age=0");

        var expiredIssuer = new JwtService(TEST_SECRET, Duration.ofMinutes(5),
                Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC));
        assertThat(get("/api/auth/me", expiredIssuer.issue(user)).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> postWithCsrf(String path, String body, String authToken) throws Exception {
        var csrfResponse = client.send(HttpRequest.newBuilder(uri("/api/security/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        var csrf = objectMapper.readTree(csrfResponse.body());
        var csrfCookie = csrfResponse.headers().allValues(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst().orElseThrow();
        var cookieHeader = csrfCookie.substring(0, csrfCookie.indexOf(';'));
        if (authToken != null) {
            cookieHeader += "; " + AuthCookieService.COOKIE_NAME + "=" + authToken;
        }
        var request = HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String authToken) throws Exception {
        var request = HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.COOKIE, AuthCookieService.COOKIE_NAME + "=" + authToken)
                .GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.createObjectNode().put("username", username).put("password", password).toString();
    }

    private String authSetCookie(HttpResponse<String> response) {
        return response.headers().allValues(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith(AuthCookieService.COOKIE_NAME + "="))
                .findFirst().orElseThrow();
    }

    private String cookieValue(String setCookie) {
        var pair = setCookie.substring(0, setCookie.indexOf(';'));
        return pair.substring(pair.indexOf('=') + 1);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
