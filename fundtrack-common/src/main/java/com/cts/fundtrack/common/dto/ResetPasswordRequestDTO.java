package com.cts.fundtrack.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for completing the password reset process.
 * <p>
 * This DTO captures the secret token (received via email) and the user's
 * requested new credentials. It is the final payload processed by the
 * Identity Service to update the user's security record.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model to finalize a password reset using a verification token")
public class ResetPasswordRequestDTO {

    /**
     * Unique token provided to the user to authorize a password reset.
     * <p>Usually a UUID or a secure hash with a 15-30 minute expiration.</p>
     */
    @NotBlank(message = "Reset token is required")
    @Schema(description = "The secret reset token sent to the user's email",
            example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
    private String token;

    /**
     * The new password the user wants to set for their account.
     */
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 12, message = "Password must be between 6 and 12 characters")
    @Schema(description = "The new account password", format = "password", example = "NewP@ssw0rd1")
    private String newPassword;
}