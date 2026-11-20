package com.lightspec.auth.service;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserEntity;
import com.lightspec.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository users;
    private final CredentialPolicy credentialPolicy;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public AuthenticationService(
            UserRepository users,
            CredentialPolicy credentialPolicy,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.credentialPolicy = credentialPolicy;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-never-used");
    }

    @Transactional(readOnly = true)
    public UserEntity authenticate(String username, String password) {
        if (!users.existsByRole(Role.ADMIN)) {
            throw new SetupRequiredException();
        }

        String canonical;
        try {
            canonical = credentialPolicy.normalizeUsername(username).canonical();
        } catch (IllegalArgumentException exception) {
            passwordEncoder.matches(password == null ? "" : password, dummyPasswordHash);
            throw new InvalidLoginException();
        }

        var user = users.findByNormalizedUsername(canonical).orElse(null);
        var hash = user == null ? dummyPasswordHash : user.getPasswordHash();
        var passwordMatches = password != null && passwordEncoder.matches(password, hash);
        if (user == null || !user.isEnabled() || !passwordMatches) {
            throw new InvalidLoginException();
        }
        return user;
    }
}
