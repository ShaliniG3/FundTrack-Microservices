package com.cts.fundtrack.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a user login request.
 *
 * <p>Carries the credentials required to authenticate a user and establish a
 * JWT-based session. Validation constraints are enforced before the payload
 * reaches the Identity Service authentication layer.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    /**
     * The registered email address of the user attempting to log in.
     * Must be a valid email format and no longer than 100 characters.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /**
     * The user's account password.
     * Must be between 6 and 100 characters. Transmitted over HTTPS only.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 12 characters")
    private String password;
}