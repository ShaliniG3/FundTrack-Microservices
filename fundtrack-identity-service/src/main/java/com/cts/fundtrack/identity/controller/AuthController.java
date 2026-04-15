package com.cts.fundtrack.identity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.dto.ForgotPasswordRequestDTO;
import com.cts.fundtrack.common.dto.ForgotPasswordResponseDTO;
import com.cts.fundtrack.common.dto.JwtResponseDTO;
import com.cts.fundtrack.common.dto.LoginRequestDTO;
import com.cts.fundtrack.common.dto.LoginResponseDTO;
import com.cts.fundtrack.common.dto.LogoutRequestDTO;
import com.cts.fundtrack.common.dto.LogoutResponseDTO;
import com.cts.fundtrack.common.dto.RegisterRequestDTO;
import com.cts.fundtrack.common.dto.RegisterResponseDTO;
import com.cts.fundtrack.common.dto.ResetPasswordRequestDTO;
import com.cts.fundtrack.common.dto.TokenRefreshRequestDTO;
import com.cts.fundtrack.common.exceptions.InvalidTokenException;
import com.cts.fundtrack.identity.model.RefreshToken;
import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.security.JwtUtil;
import com.cts.fundtrack.identity.service.AuthService;
import com.cts.fundtrack.identity.service.RefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code AuthController} exposes authentication and authorization endpoints such as
 * register, login, logout, token refresh, and password reset flows.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")	
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

        // 1. Unpacking the Optional resolves the Java(603979884) type error
        RefreshToken token = refreshTokenService.findByToken(requestToken)
                .orElseThrow(() -> {
                    log.warn("POST /auth/refresh - refresh token not found or invalid");
                    return new InvalidTokenException("Refresh token not found");
                });

        // 2. Verify and extract user
        refreshTokenService.verifyExpiration(token);
        User user = token.getUser();

        // 3. Generate new token
        String newAccessToken = jwt.generate(user.getEmail(), user.getRole().name(), user.getUserId());
        log.info("POST /auth/refresh - token refreshed successfully for role={}", user.getRole().name());

        // 4. Return explicit ResponseEntity
        return ResponseEntity.ok(
                new JwtResponseDTO(
                        "Token refreshed successfully",
                        newAccessToken,
                        requestToken,
                        user.getRole().name()
                )
        );
    }

    /**
     * Initiates the forgot password flow (e.g., send reset link).
     */
    @Operation(summary = "Forgot Password", description = "Sends a password reset link to the provided email address")
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponseDTO> forgotPassword(@Valid
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
    public ResponseEntity<String> resetPassword(@Valid
            @RequestBody ResetPasswordRequestDTO request) {

        service.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        return ResponseEntity.ok("Password reset successful");
    }
}