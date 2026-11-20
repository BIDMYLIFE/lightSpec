package com.lightspec.auth.service;

import com.lightspec.auth.repository.Role;
import com.lightspec.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupStatusService {

    private final UserRepository users;

    public SetupStatusService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public boolean isSetupRequired() {
        return !users.existsByRole(Role.ADMIN);
    }
}
