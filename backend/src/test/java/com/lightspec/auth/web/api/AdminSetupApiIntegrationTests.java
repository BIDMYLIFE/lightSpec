package com.lightspec.auth.web.api;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserEntity;
import com.lightspec.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminSetupApiIntegrationTests {

    private static final Path DATABASE = Path.of("target", "admin-api-" + UUID.randomUUID() + ".db")
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
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanUsers() {
        users.deleteAll();
    }

    @Test
    void createsAdminThroughPublicApi() throws Exception {
        var response = postAdmin("administrator", "safe-password-123", "safe-password-123");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue(HttpHeaders.LOCATION)).contains("/api/auth/me");
        assertThat(response.body()).contains("\"username\":\"administrator\"").contains("\"role\":\"ADMIN\"");
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
    }

    @Test
    void returnsProblemDetailsForInvalidFieldsWithoutWriting() throws Exception {
        var response = postAdmin("", "short", "different");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE)).hasValueSatisfying(
                value -> assertThat(value).startsWith("application/problem+json"));
        assertThat(response.body()).contains("urn:lightspec:problem:validation-failed").contains("errors");
        assertThat(users.count()).isZero();
    }

    @Test
    void returnsConflictAndDoesNotMutateDataWhenAdminExists() throws Exception {
        var original = users.saveAndFlush(
                new UserEntity("existing-admin", "existing-admin", "original-hash", Role.ADMIN));

        var response = postAdmin("second-admin", "safe-password-123", "safe-password-123");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("urn:lightspec:problem:admin-already-exists");
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findById(original.getId()).orElseThrow().getPasswordHash()).isEqualTo("original-hash");
    }

    @Test
    void commitsOnlyOneAdminWhenTwoInitializationRequestsStartTogether() throws Exception {
        var first = prepareAdminPost("first-admin", "safe-password-123", "safe-password-123");
        var second = prepareAdminPost("second-admin", "another-password-123", "another-password-123");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> sendWhenReleased(first, ready, start));
            var secondResult = executor.submit(() -> sendWhenReleased(second, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(new int[]{firstResult.get().statusCode(), secondResult.get().statusCode()})
                    .containsExactlyInAnyOrder(201, 409);
        }

        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(users.count()).isEqualTo(1);
    }

    private HttpResponse<String> postAdmin(String username, String password, String confirmation) throws Exception {
        var prepared = prepareAdminPost(username, password, confirmation);
        return prepared.client().send(prepared.request(), HttpResponse.BodyHandlers.ofString());
    }

    private PreparedRequest prepareAdminPost(String username, String password, String confirmation) throws Exception {
        var client = HttpClient.newHttpClient();
        var csrfRequest = HttpRequest.newBuilder(uri("/api/security/csrf")).GET().build();
        var csrfResponse = client.send(csrfRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode csrf = objectMapper.readTree(csrfResponse.body());
        var csrfCookie = csrfResponse.headers().allValues(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst().orElseThrow();
        var cookieHeader = csrfCookie.substring(0, csrfCookie.indexOf(';'));

        var body = objectMapper.createObjectNode()
                .put("username", username)
                .put("password", password)
                .put("passwordConfirmation", confirmation)
                .toString();
        var request = HttpRequest.newBuilder(uri("/api/setup/admin"))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return new PreparedRequest(client, request);
    }

    private HttpResponse<String> sendWhenReleased(
            PreparedRequest prepared, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("並行測試未能同步開始");
        }
        return prepared.client().send(prepared.request(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private record PreparedRequest(HttpClient client, HttpRequest request) {
    }
}
