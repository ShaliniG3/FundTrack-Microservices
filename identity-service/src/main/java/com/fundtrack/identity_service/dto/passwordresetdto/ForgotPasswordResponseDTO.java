package com.fundtrack.identity_service.dto.passwordresetdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object representing the acknowledgment of a password reset initiation.
 * <p>
 * This DTO confirms that the recovery process has started. Depending on the
 * security configuration, it provides a status message and potentially a
 * reference link for the frontend to display or redirect to.
 * </p>
 */
@Data
@Builder
@Schema(description = "Response model confirming the initiation of a password recovery workflow")
public class ForgotPasswordResponseDTO {

    /**
     * A descriptive message indicating the status of the forgot‑password request.
     */
    @Schema(description = "Status message for the user",
            example = "If an account exists with that email, a reset link has been sent.")
    private String message;

    /**
     * A generated password reset link.
     * <p>
     * Note: In high-security environments, this is often omitted from the
     * JSON response and sent exclusively via email to prevent token leakage.
     * </p>
     */
    @Schema(description = "Optional reset URL (usually sent via email instead)",
            example = "https://fundtrack.com/auth/reset?token=a1b2c3d4",
            nullable = true)
    private String resetLink;

}