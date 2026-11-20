package com.lightspec.auth.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseConfigurationTests {

    private static final Path ALTERNATE_DATABASE = Path.of(
            "target", "alternate-" + UUID.randomUUID() + ".db").toAbsolutePath();

    @DynamicPropertySource
    static void databasePath(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + ALTERNATE_DATABASE);
    }

    @Autowired
    DataSource dataSource;

    @Test
    void startsWithAlternativePathAndBusyTimeout() throws Exception {
        assertThat(Files.exists(ALTERNATE_DATABASE)).isTrue();

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA busy_timeout")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(5000);
        }
    }
}
