package com.lightspec.auth.web.api;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserEntity;
import com.lightspec.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SetupStatusApiIntegrationTests {

    private static final Path DATABASE = Path.of("target", "setup-status-" + UUID.randomUUID() + ".db")
            .toAbsolutePath();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @LocalServerPort
    int port;

    @Autowired
    UserRepository users;

    @BeforeEach
    void cleanUsers() {
        users.deleteAll();
    }

    @Test
    void publiclyReportsSetupRequiredWhenNoAdminExists() throws Exception {
        var response = getStatus();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"setupRequired\":true}");
    }

    @Test
    void publiclyReportsSetupCompleteWhenAdminExists() throws Exception {
        users.saveAndFlush(new UserEntity("Administrator", "administrator", "test-hash", Role.ADMIN));

        var response = getStatus();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"setupRequired\":false}");
    }

    private HttpResponse<String> getStatus() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/setup/status"))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
