package com.fundtrack.identity_service.controller;

import com.fundtrack.identity_service.config.JwtUtil;
import com.fundtrack.identity_service.dto.logindto.LoginRequestDTO;
import com.fundtrack.identity_service.dto.logindto.LoginResponseDTO;
import com.fundtrack.identity_service.dto.logoutdto.LogoutRequestDTO;
import com.fundtrack.identity_service.dto.logoutdto.LogoutResponseDTO;
import com.fundtrack.identity_service.dto.passwordresetdto.ForgotPasswordRequestDTO;
import com.fundtrack.identity_service.dto.passwordresetdto.ForgotPasswordResponseDTO;
import com.fundtrack.identity_service.dto.passwordresetdto.ResetPasswordRequestDTO;
import com.fundtrack.identity_service.dto.registerdto.RegisterRequestDTO;
import com.fundtrack.identity_service.dto.registerdto.RegisterResponseDTO;
import com.fundtrack.identity_service.dto.tokendto.JwtResponseDTO;
import com.fundtrack.identity_service.dto.tokendto.TokenRefreshRequestDTO;
import com.fundtrack.identity_service.exception.TokenNotFound;
import com.fundtrack.identity_service.model.RefreshToken;
import com.fundtrack.identity_service.service.AuthService;
import com.fundtrack.identity_service.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code AuthController} exposes authentication and authorization endpoints such as
 * register, login, logout, token refresh, and password reset flows.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/auth")
@Slf4j
@Tag(name = "Authentication Controller", description = "Endpoints for user onboarding, session management, and password recovery")
public class AuthController {

    private final AuthService service;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwt;

    /**
     * Registers a new user account.
     */
    @Operation(summary = "Register a new user", description = "Creates a new user account in the system and returns a summary of the created profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered",
                    content = @Content(schema = @Schema(implementation = RegisterResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid registration data provided", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("POST /auth/register - registration request received");
        RegisterResponseDTO response = service.register(request);
        log.info("POST /auth/register - registration successful");
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Authenticates a user with credentials and issues tokens.
     */
    @Operation(summary = "User Login", description = "Authenticates user credentials and issues Access and Refresh JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("POST /auth/login - login request received");
        LoginResponseDTO response = service.login(request);
        log.info("POST /auth/login - login successful, tokens issued");
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out the user and invalidates session-related state as applicable.
     */
    @Operation(summary = "User Logout", description = "Invalidates the user's session and refresh token")
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDTO> logout(@Valid @RequestBody LogoutRequestDTO request) {
        log.info("POST /auth/logout - logout request received");
        LogoutResponseDTO response = service.logout(request);
        log.info("POST /auth/logout - logout successful");
        return ResponseEntity.ok(response);
    }

    /**
     * Issues a new access token using a valid refresh token.
     */
    @Operation(summary = "Refresh Access Token", description = "Uses a valid Refresh Token to issue a new Access JWT without re-authenticating")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "403", description = "Refresh token expired or invalid", content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponseDTO> refreshToken(@Valid @RequestBody TokenRefreshRequestDTO request) {
        log.info("POST /auth/refresh - token refresh request received");
        String requestToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = jwt.generate(user.getEmail(), user.getRole().name());
                    log.info("POST /auth/refresh - token refreshed successfully for role={}", user.getRole().name());
                    return ResponseEntity.ok(
                            new JwtResponseDTO(
                                    "Token refreshed successfully",
                                    newAccessToken,
                                    requestToken,
                                    user.getRole().name()
                            )
                    );
                })
                .orElseThrow(() -> {
                    log.warn("POST /auth/refresh - refresh token not found or invalid");
                    return new TokenNotFound("Refresh token not found");
                });
    }

    /**
     * Initiates the forgot password flow (e.g., send reset link).
     */
    @Operation(summary = "Forgot Password", description = "Sends a password reset link to the provided email address")
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponseDTO> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO request) {

        ForgotPasswordResponseDTO response =
                service.forgotPassword(request.getEmail());

        return ResponseEntity.ok(response);
    }

    /**
     * Resets a user's password using a valid token.
     */
    @Operation(summary = "Reset Password", description = "Updates the user's password using the token received via email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset token", content = @Content)
    })
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordRequestDTO request) {

        service.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        return ResponseEntity.ok("Password reset successful");
    }
}