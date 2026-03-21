package com.fundtrack.identity_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity

@Data
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "password_reset_tokens",
        indexes = { @Index(columnList = "userId"), @Index(columnList = "expiresAt") })
public class PasswordResetToken {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // Store only a hash of the token (e.g., SHA-256 hex)
    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;


}


