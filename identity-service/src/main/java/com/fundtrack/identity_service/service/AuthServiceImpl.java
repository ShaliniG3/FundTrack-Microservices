package com.fundtrack.identity_service.service;

import com.fundtrack.identity_service.config.JwtUtil;
import com.fundtrack.identity_service.dto.auditdto.AuditLogRequestDTO;
import com.fundtrack.identity_service.dto.logindto.LoginRequestDTO;
import com.fundtrack.identity_service.dto.logindto.LoginResponseDTO;
import com.fundtrack.identity_service.dto.logoutdto.LogoutRequestDTO;
import com.fundtrack.identity_service.dto.logoutdto.LogoutResponseDTO;
import com.fundtrack.identity_service.dto.passwordresetdto.ForgotPasswordResponseDTO;
import com.fundtrack.identity_service.dto.registerdto.RegisterRequestDTO;
import com.fundtrack.identity_service.dto.registerdto.RegisterResponseDTO;
import com.fundtrack.identity_service.exception.*;
import com.fundtrack.identity_service.model.*;
import com.fundtrack.identity_service.repository.UserRepository;
import com.fundtrack.identity_service.client.AuditClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation for authentication and user session workflows.
 * <p>
 * Supports user registration, login, logout, and password reset flows.
 * Emits audit logs via {@code AuditService} and issues JWT access/refresh tokens
 * through {@link JwtUtil} and {@link RefreshTokenServiceImpl}.
 * <p>
 * Logging is added for observability while avoiding sensitive data (e.g., tokens, passwords).
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditClient auditClient;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final Map<String, String> resetTokens = new HashMap<>();
    private final RefreshTokenService refreshTokenService;
    @Value("${app.password_reset_url}")
    private String passwordResetURL;




    /*
     * =========================
     * REGISTER
     * =========================
     */

    /**
     * Registers a new user, persists it, and writes an audit log.
     *
     * @param request the registration payload with name, email, phone, password, and role
     * @return a summary of the newly registered user
     * @throws DuplicateResourceException if email or phone already exists
     * @throws InvalidRoleException       if the provided role is invalid
     */
    @Override
    public RegisterResponseDTO register(RegisterRequestDTO request) {

        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        String phone = request.getPhoneNumber() == null ? null : request.getPhoneNumber().trim().replaceAll("\\s+", "");
        request.setEmail(email);
        request.setPhoneNumber(phone);

        log.info("Register request received for email={}", email);

        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Registration failed: duplicate email={}", email);
            // This will be handled by GlobalExceptionHandler and returned as ApiResponse
            throw new DuplicateResourceException("Email already registered");
        }
        if (phone != null && userRepository.existsByPhone((phone))) {
            log.warn("Registration failed: duplicate phone={}", phone);
            throw new DuplicateResourceException("Phone number already registered");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: invalid role provided role={}", request.getRole());
            throw new InvalidRoleException("Invalid role: " + request.getRole());
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(phone)
                .role(role)
                .status(LoginStatus.LOGGED_OUT)
                .build();

        User savedUser = userRepository.save(user);
        try {
            AuditLogRequestDTO auditRequest = new AuditLogRequestDTO();
            auditRequest.setAction(ActionType.REGISTER);
            auditRequest.setEntityName(EntityType.USER);
            auditRequest.setEntityId(savedUser.getUserId());
            auditRequest.setUserId(savedUser.getUserId());

            auditClient.log(auditRequest);
            log.info("Audit log sent to Audit Service for userId={}", savedUser.getUserId());
        } catch (Exception e) {
            log.error("Failed to log registration to Audit Service: {}", e.getMessage());
        }

        log.info("Registration successful: userId={}, role={}", savedUser.getUserId(), savedUser.getRole());

        return RegisterResponseDTO.builder()
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .build();

    }

    /*
     * =========================
     * LOGIN
     * =========================
     */

    /**
     * Authenticates a user, issues JWT tokens, updates login status, and writes an audit log.
     *
     * @param request the login payload containing email and password
     * @return a response with access token, refresh token, expiry, and user profile info
     * @throws UserNotFoundException               if the user is not found
     * @throws UserAlreadyLoggedInException    if the user is already logged in
     * @throws org.springframework.security.core.AuthenticationException if authentication fails
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {

        log.info("Login attempt for email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found for email={}", request.getEmail());
                    return new UserNotFoundException("User not Registered. Please register first.");
                });

        if (user.getStatus() == LoginStatus.LOGGED_IN) {
            log.warn("Login blocked: user already logged in email={}", user.getEmail());
            throw new UserAlreadyLoggedInException("User already logged in");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            log.error("Auth failed for {}: {}", request.getEmail(), e.getMessage());
            throw e; // GlobalExceptionHandler will catch this
        }

        String roleClaim = user.getRole().name(); // e.g., "ADMIN"

        // Generate JWT (match JwtUtil signature: (email, role))
        String accessToken = jwtUtil.generate(user.getEmail(), roleClaim);

        // Get expiration from the token
        Date expiresAt = jwtUtil.getExpirationTime(accessToken);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        // Update login status
        user.setStatus(LoginStatus.LOGGED_IN);
        userRepository.save(user);
        try {
            AuditLogRequestDTO auditRequest = new AuditLogRequestDTO();
            auditRequest.setAction(ActionType.LOGIN);
            auditRequest.setEntityName(EntityType.USER);
            auditRequest.setEntityId(user.getUserId());
            auditRequest.setUserId(user.getUserId());

            auditClient.log(auditRequest);
            log.info(" Login audit log sent to Audit Service for userId={}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to log login to Audit Service: {}", e.getMessage());
        }

        log.info("Login successful: userId={}, role={}, expiresAt={}", user.getUserId(), roleClaim, expiresAt);

        // Build response (adjust types to match your DTO)
        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(expiresAt.getTime()) // epoch millis at expiration
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

    }

    /*
     * =========================
     * LOGOUT
     * =========================
     */

    /**
     * Logs out the user by updating their status and writes an audit log.
     *
     * @param request the logout payload containing the user's email
     * @return a response confirming the logout
     * @throws UserNotFoundException                 if the user email is not registered
     * @throws UserAlreadyLoggedOutException     if the user is already logged out
     */
    @Override
public LogoutResponseDTO logout(LogoutRequestDTO request) {
    log.info("Logout request for email={}", request.getEmail());

    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UserNotFoundException("User not registered"));

    if (user.getStatus() == LoginStatus.LOGGED_OUT) {
        throw new UserAlreadyLoggedOutException("User already logged out");
    }

    // 🔑 Delete all refresh tokens for this user
    refreshTokenService.deleteByUser(user);

    // Update user status
    user.setStatus(LoginStatus.LOGGED_OUT);
    userRepository.save(user);

        try {
            AuditLogRequestDTO auditRequest = new AuditLogRequestDTO();
            auditRequest.setAction(ActionType.LOGOUT);
            auditRequest.setEntityName(EntityType.USER);
            auditRequest.setEntityId(user.getUserId());
            auditRequest.setUserId(user.getUserId());

            auditClient.log(auditRequest);
            log.info(" Logout audit log sent to Audit Service for userId={}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to log logout to Audit Service: {}", e.getMessage());
        }

    return LogoutResponseDTO.builder()
            .email(user.getEmail())
            // .message("Logout successful, tokens revoked")
            .build();
}

    /**
     * Initiates a forgot-password flow by generating a reset link (token-based).
     *
     * @param email the email address of the user requesting password reset
     * @return a response containing a generic message and a reset link (implementation-specific)
     * @throws RuntimeException if no user is found for the given email
     */
    public ForgotPasswordResponseDTO forgotPassword(String email) {

        log.info("Forgot password request received for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Forgot password: user not found for email={}", email);
                    return new UserNotFoundException("User not found");
                });

        String token = UUID.randomUUID().toString();

        resetTokens.put(token, email);

        String resetLink =
                passwordResetURL.replace("{token}", token);

        log.info("Password reset link generated for userId={}", user.getUserId());
        // Intentionally not logging token or full link for security

        return ForgotPasswordResponseDTO.builder()
                .message("Reset link generated successfully")
                .resetLink(resetLink)
                .build();
    }

    /**
     * Resets the user's password using a previously generated token.
     *
     * @param token       the reset token associated with the forgot-password request
     * @param newPassword the new password to be set
     * @throws RuntimeException if the token is invalid or the user cannot be found
     */
    public void resetPassword(String token, String newPassword) {

        log.info("Password reset attempt with token (hidden for security)");

        String email = resetTokens.get(token);

        if (email == null) {
            log.warn("Password reset failed: invalid token");
            throw new InvalidTokenException("Invalid token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Password reset failed: user not found for email={}", email);
                    return new UserNotFoundException("User not found");
                });

        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);
        try {
            AuditLogRequestDTO auditRequest = new AuditLogRequestDTO();
            auditRequest.setAction(ActionType.RESET_PASSWORD);
            auditRequest.setEntityName(EntityType.USER);
            auditRequest.setEntityId(user.getUserId());
            auditRequest.setUserId(user.getUserId());

            auditClient.log(auditRequest);
            log.info(" Reset password audit log sent to Audit Service for userId={}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to log Reset password to Audit Service: {}", e.getMessage());
        }
        resetTokens.remove(token);

        log.info("Password reset successful for userId={}", user.getUserId());
    }


}



