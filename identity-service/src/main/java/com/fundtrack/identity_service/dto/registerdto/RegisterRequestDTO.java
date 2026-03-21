package com.fundtrack.identity_service.dto.registerdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the registration request submitted by a new user.
 * <p>
 * This DTO captures the comprehensive profile required to initialize a new account.
 * Validation is enforced here to maintain data integrity and security compliance
 * before the payload reaches the User Persistence layer.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model for creating a new user account with role-based access")
public class RegisterRequestDTO {

    /**
     * Full name of the user registering.
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Schema(description = "Legal full name of the registrant", example = "John Doe")
    private String name;

    /**
     * Email address for the new user account.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Primary email address (used for login)", example = "john.doe@cts.com")
    private String email;

    /**
     * Password for the new account.
     * Enforces complexity: Uppercase, Lowercase, and Digit.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 12, message = "Password must be between 8 and 100 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    @Schema(description = "Account password requiring mixed-case and numbers",
            format = "password", example = "P@ssw0rd123")
    private String password;

    /**
     * Role assigned to the user.
     * Supports various administrative and applicant types.
     */
    @NotBlank(message = "Role is required")
    @Pattern(
            regexp = "^(?i)(ROLE_)?(USER|ADMIN|FINANCE[_\\-\\s]?OFFICER|APPLICANT|APPROVER|REVIEWER)$",
            message = "Role must be one of: USER, ADMIN, FINANCE_OFFICER, APPLICANT, APPROVER, REVIEWER"
    )
    @Schema(description = "The authorization role for the new user",
            allowableValues = {"USER", "ADMIN", "FINANCE_OFFICER", "APPLICANT", "APPROVER", "REVIEWER"},
            example = "APPLICANT")
    private String role;

    /**
     * Phone number of the user.
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must be exactly 10 digits"
    )
    @Schema(description = "10-digit mobile or landline number", example = "9876543210")
    private String phoneNumber;

}