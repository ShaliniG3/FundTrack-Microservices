package com.cts.fundtrack.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for initiating a password recovery process.
 * <p>
 * This DTO captures the user's identity (email) to trigger the generation
 * of a secure reset token or OTP. For security reasons, the backend should
 * process this request without revealing whether the email exists in the database.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model to initiate a password reset via email")
public class ForgotPasswordRequestDTO {

    /**
     * Email of the user requesting a password reset.
     */
    @NotBlank(message = "Email must not be null")
    @Email(message = "Invalid email format")
    @Schema(description = "The registered email address where the reset link will be sent",
            example = "user@cts.fundtrack.com")
    private String email;
}