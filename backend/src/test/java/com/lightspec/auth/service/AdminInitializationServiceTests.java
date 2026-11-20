package com.lightspec.auth.service;

import com.lightspec.auth.repository.BootstrapGuardRepository;
import com.lightspec.auth.repository.Role;
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
class AdminInitializationServiceTests {

    private static final Path DATABASE = Path.of("target", "admin-service-" + UUID.randomUUID() + ".db")
            .toAbsolutePath();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Autowired
    AdminInitializationService service;

    @Autowired
    UserRepository users;

    @Autowired
    BootstrapGuardRepository guards;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUsers() {
        users.deleteAll();
    }

    @Test
    void createsExactlyOneEnabledAdminWithArgon2idPassword() {
        var initialGuardVersion = guards.findById(1).orElseThrow().getVersion();

        var created = service.initialize("  管理員  ", "safe-password-123", "safe-password-123");

        assertThat(created.getUsername()).isEqualTo("管理員");
        assertThat(created.getNormalizedUsername()).isEqualTo("管理員");
        assertThat(created.getRole()).isEqualTo(Role.ADMIN);
        assertThat(created.isEnabled()).isTrue();
        assertThat(created.getPasswordHash()).startsWith("$argon2id$");
        assertThat(created.getPasswordHash()).doesNotContain("safe-password-123");
        assertThat(passwordEncoder.matches("safe-password-123", created.getPasswordHash())).isTrue();
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(guards.findById(1).orElseThrow().getVersion()).isGreaterThan(initialGuardVersion);
    }

    @Test
    void rejectsInvalidCredentialsWithoutWritingAUser() {
        assertThatThrownBy(() -> service.initialize("ab", "safe-password-123", "safe-password-123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.initialize("administrator", "short", "short"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.initialize("administrator", "safe-password-123", "different-password"))
                .isInstanceOf(PasswordConfirmationException.class);

        assertThat(users.count()).isZero();
    }

    @Test
    void rejectsASecondInitializationWithoutChangingExistingAdmin() {
        var first = service.initialize("first-admin", "safe-password-123", "safe-password-123");

        assertThatThrownBy(() -> service.initialize("second-admin", "another-password-123", "another-password-123"))
                .isInstanceOf(AdminAlreadyExistsException.class);

        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findById(first.getId())).isPresent();
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
    }
}
