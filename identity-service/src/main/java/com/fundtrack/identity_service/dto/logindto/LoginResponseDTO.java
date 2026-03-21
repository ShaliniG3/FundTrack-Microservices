package com.fundtrack.identity_service.dto.logindto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response model containing JWT tokens and user profile metadata after successful login")
public class LoginResponseDTO {

    /**
     * JWT access token issued upon successful authentication.
     */
    @Schema(description = "JWT Access Token for authorizing API requests",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    /**
     * Expiration timestamp of the access token in epoch milliseconds.
     */
    @Schema(description = "Token lifetime in milliseconds", example = "3600000")
    private long expiresIn;

    /**
     * Unique identifier of the authenticated user.
     */
    @Schema(description = "Unique ID of the user", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID userId;

    /**
     * Full name of the authenticated user.
     */
    @Schema(description = "Full name of the user for UI display", example = "Jane Smith")
    private String name;

    /**
     * Email address of the authenticated user.
     */
    @Schema(description = "Registered email address of the user", example = "jane.smith@cts.com")
    private String email;

    /**
     * Refresh token used to generate a new access token.
     */
    @Schema(description = "Token used to refresh the session without re-logging",
            example = "d7b2c3a4-e5f6-7g8h-9i0j...")
    private String refreshToken;

    /**
     * Role assigned to the user.
     */
    @Schema(description = "The security role assigned to the user", example = "COMPLIANCE_OFFICER")
    private String role;
}