package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Data Transfer Object returned after a successful forgot-password request.
 *
 * <p>Confirms to the client that the reset process has been initiated. The
 * {@code resetLink} is included for development/testing environments; in production
 * it is typically omitted from the response and sent only via email for security.</p>
 *
 * <p>The {@code userId} field is included so that the {@code AuditAspect} can extract
 * the actor's identity for audit logging without requiring a separate lookup.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResponseDTO {

    /** Human-readable confirmation message (e.g., "Password reset link sent to your email"). */
    private String message;

    /**
     * The password reset URL containing the one-time token.
     * Should only be included in non-production environments.
     */
    private String resetLink;

    /**
     * Unique identifier of the user who initiated the reset request.
     * Used by the {@code AuditAspect} to associate the audit entry with the correct user.
     */
    private UUID userId;
}