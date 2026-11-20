package com.lightspec.auth.service;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserEntity;
import com.lightspec.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserRegistrationServiceTests {

    private static final Path DATABASE = Path.of("target", "registration-" + UUID.randomUUID() + ".db")
            .toAbsolutePath();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Autowired
    UserRegistrationService service;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUsers() {
        users.deleteAll();
    }

    @Test
    void registrationIsUnavailableUntilAnAdminExists() {
        assertThatThrownBy(() -> service.register("new-user", "safe-password-123", "safe-password-123"))
                .isInstanceOf(SetupRequiredException.class);
        assertThat(users.count()).isZero();
    }

    @Test
    void createsOnlyAUserAndNeverStoresPlaintextPassword() {
        users.saveAndFlush(new UserEntity("admin", "admin", "admin-hash", Role.ADMIN));

        var created = service.register("  New-User  ", "safe-password-123", "safe-password-123");

        assertThat(created.getUsername()).isEqualTo("New-User");
        assertThat(created.getNormalizedUsername()).isEqualTo("new-user");
        assertThat(created.getRole()).isEqualTo(Role.USER);
        assertThat(created.getPasswordHash()).startsWith("$argon2id$");
        assertThat(created.getPasswordHash()).doesNotContain("safe-password-123");
        assertThat(passwordEncoder.matches("safe-password-123", created.getPasswordHash())).isTrue();
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(users.countByRole(Role.USER)).isEqualTo(1);
    }

    @Test
    void rejectsEquivalentDuplicateUsername() {
        users.saveAndFlush(new UserEntity("admin", "admin", "admin-hash", Role.ADMIN));
        service.register("New-User", "safe-password-123", "safe-password-123");

        assertThatThrownBy(() -> service.register("new-user", "another-password-123", "another-password-123"))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        assertThat(users.countByRole(Role.USER)).isEqualTo(1);
    }
}
