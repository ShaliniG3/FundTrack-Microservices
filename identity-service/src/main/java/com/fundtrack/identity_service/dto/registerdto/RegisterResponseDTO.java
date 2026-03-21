package com.fundtrack.identity_service.dto.registerdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing the outcome of a successful user registration.
 * <p>
 * This DTO returns the non-sensitive profile details of the newly created account.
 * It is primarily used by the client to confirm account creation and to initialize
 * local user sessions or state management.
 * </p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response model providing a summary of the newly created user account")
public class RegisterResponseDTO {

    /**
     * Unique identifier assigned to the newly registered user.
     */
    @Schema(description = "The system-generated unique ID for the user",
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID userId;

    /**
     * Full name of the registered user.
     */
    @Schema(description = "The registered full name", example = "John Doe")
    private String name;

    /**
     * Email address of the user.
     */
    @Schema(description = "The registered primary email address", example = "john.doe@cts.com")
    private String email;

    /**
     * Role assigned to the user.
     */
    @Schema(description = "The authorization role granted to the user", example = "APPLICANT")
    private String role;

    /**
     * Phone number provided by the user during registration.
     */
    @Schema(description = "The registered contact number", example = "9876543210")
    private String phone;

}