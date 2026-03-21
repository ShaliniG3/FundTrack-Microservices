package com.fundtrack.identity_service.dto.tokendto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for requesting a new JWT access token.
 * <p>
 * This DTO is used when an existing access token has expired. The client
 * provides a valid, non-expired refresh token to obtain a fresh identity
 * and access pair, maintaining the user's session continuity.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model to rotate an expired access token using a refresh token")
public class TokenRefreshRequestDTO {

    /**
     * The refresh token used to request a new access token.
     */
    @NotBlank(message = "Refresh token must not be empty")
    @Schema(description = "The long-lived refresh token previously issued by the server",
            example = "d8f1e2a3-b4c5-d6e7-f8g9-h0i1j2k3l4m5")
    private String refreshToken;
}