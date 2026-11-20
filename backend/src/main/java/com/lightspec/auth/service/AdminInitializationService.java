package com.lightspec.auth.service;

import com.lightspec.auth.repository.BootstrapGuardRepository;
import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserEntity;
import com.lightspec.auth.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminInitializationService {

    private final UserRepository users;
    private final BootstrapGuardRepository guards;
    private final EntityManager entityManager;
    private final CredentialPolicy credentialPolicy;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializationService(
            UserRepository users,
            BootstrapGuardRepository guards,
            EntityManager entityManager,
            CredentialPolicy credentialPolicy,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.guards = guards;
        this.entityManager = entityManager;
        this.credentialPolicy = credentialPolicy;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity initialize(String username, String password, String passwordConfirmation) {
        var guard = guards.findById(1)
                .orElseThrow(() -> new IllegalStateException("找不到初始化鎖定記錄"));
        entityManager.lock(guard, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

        if (users.existsByRole(Role.ADMIN)) {
            throw new AdminAlreadyExistsException();
        }

        var normalizedUsername = credentialPolicy.normalizeUsername(username);
        credentialPolicy.validatePassword(password);
        if (!password.equals(passwordConfirmation)) {
            throw new PasswordConfirmationException();
        }
        if (users.existsByNormalizedUsername(normalizedUsername.canonical())) {
            throw new UsernameAlreadyExistsException();
        }

        var admin = new UserEntity(
                normalizedUsername.display(),
                normalizedUsername.canonical(),
                passwordEncoder.encode(password),
                Role.ADMIN);
        return users.saveAndFlush(admin);
    }
}
