package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Data Transfer Object returned after a successful logout operation.
 *
 * <p>Confirms the identity of the session that was terminated. The {@code userId}
 * field is included so that the {@code AuditAspect} can extract the actor's identity
 * for audit logging without an additional lookup against the Identity Service.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResponseDTO {

    /** Email address of the user whose session was terminated. */
    private String email;

    /**
     * Unique identifier of the user who logged out.
     * Used by the {@code AuditAspect} to associate the audit entry with the correct user.
     */
    private UUID userId;
}