package com.lightspec.auth.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTests {

    @TempDir
    Path tempDir;

    @Test
    void migratesFreshSqliteDatabase() throws Exception {
        var database = tempDir.resolve("fresh.db");
        var url = "jdbc:sqlite:" + database;

        var result = Flyway.configure()
                .dataSource(url, null, null)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(result.migrationsExecuted).isEqualTo(1);

        try (var connection = DriverManager.getConnection(url)) {
            assertThat(objectExists(connection, "table", "users")).isTrue();
            assertThat(objectExists(connection, "table", "bootstrap_guard")).isTrue();
            assertThat(objectExists(connection, "index", "uk_users_normalized_username")).isTrue();

            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT id, version FROM bootstrap_guard")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("id")).isEqualTo(1L);
                assertThat(rows.getLong("version")).isZero();
                assertThat(rows.next()).isFalse();
            }
        }
    }

    private boolean objectExists(java.sql.Connection connection, String type, String name) throws Exception {
        try (var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?")) {
            statement.setString(1, type);
            statement.setString(2, name);
            try (var rows = statement.executeQuery()) {
                return rows.next() && rows.getInt(1) == 1;
            }
        }
    }
}
