package com.fundtrack.identity_service.dto.tokendto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the bearer tokens issued upon successful authentication.
 * <p>
 * This payload provides the necessary credentials for subsequent authorized API calls.
 * It includes a short-lived access token and a refresh token used to extend the session
 * without requiring the user to re-enter their credentials.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model containing JWT access and refresh tokens")
public class JwtResponseDTO {

    /**
     * Descriptive message indicating the outcome of the token operation.
     */
    @Schema(description = "Status message of the authentication or refresh process",
            example = "Authentication successful")
    private String message;

    /**
     * Newly generated JWT access token for authenticated API access.
     */
    @Schema(description = "The short-lived JWT used in the 'Authorization' header",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    /**
     * Refresh token used to obtain new access tokens without re-authentication.
     */
    @Schema(description = "The long-lived token used to regenerate an access token",
            example = "d8f1e2a3-b4c5-d6e7-f8g9...")
    private String refreshToken;

    /**
     * The role assigned to the authenticated user.
     */
    @Schema(description = "The primary role of the user, used for client-side routing",
            example = "FINANCE_OFFICER")
    private String role;
}