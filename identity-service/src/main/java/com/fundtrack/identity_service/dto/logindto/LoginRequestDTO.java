package com.fundtrack.identity_service.dto.logindto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a login request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for user authentication")
public class LoginRequestDTO {

    /**
     * The email address of the user attempting to log in.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(
            description = "Registered email address of the user",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    /**
     * The password associated with the user's account.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 12, message = "Password must be between 6 and 12 characters")
    @Schema(
            description = "User password",
            example = "Pass123!",
            minLength = 6,
            maxLength = 12,
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "password"
    )
    private String password;
}