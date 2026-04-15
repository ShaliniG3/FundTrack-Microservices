package com.cts.fundtrack.identity.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.cts.fundtrack.common.models.enums.LoginStatus;
import com.cts.fundtrack.common.models.enums.Role;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Core security entity representing a system user.
 * <p>
 * This class implements {@link UserDetails} to integrate directly with Spring Security.
 * It manages authentication credentials and role-based access control (RBAC).
 * </p>
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_phone", columnList = "phone")
        })
public class User implements UserDetails {

    /**
     * Unique system identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    /**
     * The full legal name of the user.
     */
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Unique email address used as the primary username for authentication.
     */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Unique contact phone number.
     */
    @Column(length = 30, unique = true)
    private String phone;

    /**
     * Bcrypt encoded password string.
     */
    @Column(nullable = false, unique = true)
    private String password;

    /**
     * The primary authorization level assigned to the user.
     */
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * Audit timestamp indicating when the account was first created.
     */
    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Current account state (e.g., ACTIVE, INACTIVE).
     */
    @Enumerated(EnumType.STRING)
    private LoginStatus status;

    /**
     * Maps the internal {@link Role} to Spring Security GrantedAuthorities.
     * Prefixes the role name with 'ROLE_' for standard intercept-url compatibility.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    /**
     * Returns the email as the primary principal for the security context.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Can be linked to {@link LoginStatus} to disable accounts programmatically.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}