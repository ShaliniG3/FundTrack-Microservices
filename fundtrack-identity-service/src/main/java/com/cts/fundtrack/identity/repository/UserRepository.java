package com.cts.fundtrack.identity.repository;

import com.cts.fundtrack.identity.model.User; // Updated to point to the Identity Service model
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for performing CRUD and query operations on {@link User} entities.
 * <p>
 * Extends {@link JpaRepository} to provide built‑in JPA methods and defines
 * additional query methods used by authentication and registration flows.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     *
     * @param email the user's email
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their phone number.
     *
     * @param phone the user's phone number
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByPhone(String phone);

    /**
     * Checks if a user already exists with the provided email,
     * ignoring case sensitivity.
     *
     * @param email the email to check
     * @return {@code true} if a user exists with this email, otherwise {@code false}
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks if a user already exists with the provided phone number.
     *
     * @param phone the phone number to check
     * @return {@code true} if a user exists with this phone number, otherwise {@code false}
     */
    boolean existsByPhone(String phone);
}