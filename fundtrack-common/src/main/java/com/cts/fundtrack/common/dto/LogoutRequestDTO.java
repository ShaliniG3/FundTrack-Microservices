package com.cts.fundtrack.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a request to terminate a user session.
 * <p>
 * This DTO is used to identify the user session that should be invalidated.
 * It is typically processed by the Security Service to clear security contexts
 * or add active tokens to a revocation list (blacklisting).
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model to initiate a secure logout for a specific user")
public class LogoutRequestDTO {

    /**
     * The email address of the user requesting logout.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "The registered email address of the user logging out",
            example = "admin@fundtrack.com")
    private String email;
}