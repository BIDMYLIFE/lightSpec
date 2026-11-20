package com.lightspec.auth.service;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserEntity;
import com.lightspec.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private final UserRepository users;
    private final CredentialPolicy credentialPolicy;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserRepository users,
            CredentialPolicy credentialPolicy,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.credentialPolicy = credentialPolicy;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity register(String username, String password, String passwordConfirmation) {
        if (!users.existsByRole(Role.ADMIN)) {
            throw new SetupRequiredException();
        }

        var normalizedUsername = credentialPolicy.normalizeUsername(username);
        credentialPolicy.validatePassword(password);
        if (!password.equals(passwordConfirmation)) {
            throw new PasswordConfirmationException();
        }
        if (users.existsByNormalizedUsername(normalizedUsername.canonical())) {
            throw new UsernameAlreadyExistsException();
        }

        var user = new UserEntity(
                normalizedUsername.display(),
                normalizedUsername.canonical(),
                passwordEncoder.encode(password),
                Role.USER);
        return users.saveAndFlush(user);
    }
}
