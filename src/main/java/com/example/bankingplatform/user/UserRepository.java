package com.example.bankingplatform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists (case insensitive)
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Check if email exists (case insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);
}