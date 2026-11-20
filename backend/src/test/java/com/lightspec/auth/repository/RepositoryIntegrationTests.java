package com.lightspec.auth.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.datasource.driver-class-name=org.sqlite.JDBC"
})
@ActiveProfiles("test")
class RepositoryIntegrationTests {

    private static final Path DATABASE = Path.of("target", "repository-" + UUID.randomUUID() + ".db")
            .toAbsolutePath();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Autowired
    UserRepository users;

    @Autowired
    BootstrapGuardRepository guards;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanUsers() {
        users.deleteAll();
    }

    @Test
    void queriesAdminAndEnforcesNormalizedUsernameUniqueness() {
        users.saveAndFlush(new UserEntity("Admin", "admin", "hash-one", Role.ADMIN));

        assertThat(users.existsByRole(Role.ADMIN)).isTrue();
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(users.findByNormalizedUsername("admin")).isPresent();

        assertThatThrownBy(() -> users.saveAndFlush(
                new UserEntity("ADMIN", "admin", "hash-two", Role.USER)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void bootstrapGuardSupportsOptimisticForceIncrement() {
        var initialVersion = guards.findById(1).orElseThrow().getVersion();
        var transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            var guard = guards.findById(1).orElseThrow();
            entityManager.lock(guard, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        });

        assertThat(guards.findById(1).orElseThrow().getVersion()).isGreaterThan(initialVersion);
    }
}
