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
 * REST controller that exposes all public-facing authentication and session-management
 * endpoints for the FundTrack Identity Service.
 *
 * <p>Covered flows:</p>
 * <ul>
 *   <li><b>Register</b> — create a new user account.</li>
 *   <li><b>Login</b> — validate credentials and issue a JWT access token plus a
 *       refresh token.</li>
 *   <li><b>Logout</b> — invalidate the refresh token and update the user's login
 *       status.</li>
 *   <li><b>Token Refresh</b> — exchange a valid refresh token for a new access
 *       JWT without re-authenticating.</li>
 *   <li><b>Forgot Password</b> — generate and dispatch a one-time password-reset
 *       link to the user's email.</li>
 *   <li><b>Reset Password</b> — consume the one-time token and update the
 *       user's password.</li>
 * </ul>
 *
 * <p>All endpoints under {@code /api/v1/auth} are whitelisted in
 * {@link com.cts.fundtrack.identity.config.SecurityConfig} and do not require a
 * JWT Bearer token.</p>
 *
 * @see AuthService
 * @see RefreshTokenService
 * @see JwtUtil
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
     * Registers a new user account in the system.
     *
     * <p>Validates that neither the email address nor the phone number is already
     * in use before persisting the new {@link User} record. The password is
     * BCrypt-encoded before storage. On success, a summary of the created profile
     * is returned with HTTP 201.</p>
     *
     * @param request the validated registration payload containing name, email,
     *                phone number, password, and desired role
     * @return {@code 201 Created} with a {@link RegisterResponseDTO} describing
     *         the newly created user
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
     * Authenticates a user with their email and password and issues JWT tokens.
     *
     * <p>On successful authentication, two tokens are returned:</p>
     * <ul>
     *   <li>A short-lived <b>access token</b> (JWT) for authorising API calls.</li>
     *   <li>A long-lived <b>refresh token</b> for obtaining new access tokens
     *       without re-entering credentials.</li>
     * </ul>
     * <p>The user's {@code LoginStatus} is updated to {@code LOGGED_IN}.</p>
     *
     * @param request the validated login payload containing the user's email and
     *                plaintext password
     * @return {@code 200 OK} with a {@link LoginResponseDTO} containing both tokens
     *         and basic profile information
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
     * Logs out the authenticated user and invalidates their refresh token.
     *
     * <p>All refresh tokens associated with the user are deleted from the database,
     * and the user's {@code LoginStatus} is updated to {@code LOGGED_OUT}. The
     * short-lived access JWT will continue to be accepted until it naturally expires;
     * callers should discard it client-side immediately after logout.</p>
     *
     * @param request the validated logout payload containing the user's email
     * @return {@code 200 OK} with a {@link LogoutResponseDTO} confirming the logout
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
     * Issues a new access JWT using a valid, unexpired refresh token.
     *
     * <p>Steps performed:</p>
     * <ol>
     *   <li>Locate the {@link RefreshToken} by its token string value.</li>
     *   <li>Verify that the refresh token has not expired; delete and reject if it has.</li>
     *   <li>Generate a fresh access JWT signed with the user's email, role, and UUID.</li>
     *   <li>Return the new access token alongside the original (unchanged) refresh token.</li>
     * </ol>
     *
     * @param request the validated payload containing the {@code refreshToken} string
     * @return {@code 200 OK} with a {@link JwtResponseDTO} containing the new access token
     * @throws InvalidTokenException if the refresh token is not found or has expired
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
     * Initiates the forgot-password flow by generating a one-time reset link.
     *
     * <p>A random UUID token is created, stored in memory keyed to the user's
     * email, and embedded into a password-reset URL that is returned in the
     * response. In a production system this URL would typically be emailed to
     * the user rather than returned directly.</p>
     *
     * @param request the validated payload containing the user's registered email address
     * @return {@code 200 OK} with a {@link ForgotPasswordResponseDTO} containing
     *         the user's UUID and the generated reset link
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
     * Resets a user's password using a valid one-time reset token.
     *
     * <p>The token must have been previously generated by
     * {@link #forgotPassword(ForgotPasswordRequestDTO)}. Once consumed successfully,
     * the token is invalidated and cannot be reused. The new password is
     * BCrypt-encoded before being persisted.</p>
     *
     * @param request the validated payload containing the one-time {@code token}
     *                and the desired {@code newPassword}
     * @return {@code 200 OK} with a plain-text confirmation message
     * @throws InvalidTokenException if the provided token is not found or has already been used
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
