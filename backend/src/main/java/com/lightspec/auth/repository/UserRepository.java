package com.lightspec.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    boolean existsByRole(Role role);

    long countByRole(Role role);

    boolean existsByNormalizedUsername(String normalizedUsername);

    Optional<UserEntity> findByNormalizedUsername(String normalizedUsername);
}
