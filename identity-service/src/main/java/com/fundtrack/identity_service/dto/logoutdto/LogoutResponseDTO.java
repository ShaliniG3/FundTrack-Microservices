package com.fundtrack.identity_service.dto.logoutdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the successful outcome of a logout operation.
 * <p>
 * This DTO provides a final confirmation of the account that was logged out.
 * It is typically used by the frontend to trigger local state cleanup (e.g.,
 * clearing localStorage or Redux stores) and redirect the user to the login page.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model confirming the successful termination of a user session")
public class LogoutResponseDTO {

    /**
     * The email address of the user who has been successfully logged out.
     */
    @Schema(description = "The email address associated with the terminated session",
            example = "user@cts.fundtrack.com")
    private String email;

}