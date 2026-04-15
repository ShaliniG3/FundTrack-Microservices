package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing the response returned after a successful login.
 * <p>
 * This DTO encapsulates the JWT security context and basic profile metadata
 * required by the client to initialize an authenticated session.
 * </p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    /**
     * JWT access token issued upon successful authentication.
     */
    private String accessToken;

    /**
     * Expiration timestamp of the access token in epoch milliseconds.
     */
    private long expiresIn;

    /**
     * Unique identifier of the authenticated user.
     */
    private UUID userId;

    /**
     * Full name of the authenticated user.
     */
    private String name;

    /**
     * Email address of the authenticated user.
     */
    private String email;

    /**
     * Refresh token used to generate a new access token.
     */
    private String refreshToken;

    /**
     * Role assigned to the user.
     */
    private String role;
}