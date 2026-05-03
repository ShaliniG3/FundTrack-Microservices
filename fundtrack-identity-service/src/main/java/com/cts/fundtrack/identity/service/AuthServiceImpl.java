package com.cts.fundtrack.identity.service;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.*;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.LoginStatus;
import com.cts.fundtrack.common.models.enums.Role;
import com.cts.fundtrack.identity.model.RefreshToken;
import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.repository.UserRepository;
import com.cts.fundtrack.identity.security.JwtUtil;
import com.cts.fundtrack.common.exceptions.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Primary implementation of {@link AuthService} that handles all authentication
 * and account-management operations for the FundTrack Identity Service.
 *
 * <p>Every public method that mutates state is annotated with {@code @Auditable}
 * so the AOP {@link com.cts.fundtrack.identity.aspect.AuditAspect} automatically
 * records a corresponding {@link com.cts.fundtrack.identity.model.AuditLog} entry
 * on successful completion.</p>
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>The entire class is marked {@code @Transactional} so that multi-step
 *       operations (e.g., saving the user <em>and</em> updating the login status)
 *       are atomic.</li>
 *   <li>Password reset tokens are stored in an in-memory {@link HashMap}. This is
 *       sufficient for single-instance deployments but should be replaced with a
 *       distributed store (e.g., Redis) for production multi-instance setups.</li>
 *   <li>The {@link AuthenticationManager} is used for credential verification
 *       during login, delegating to
 *       {@link com.cts.fundtrack.identity.security.CustomUserDetailsService}.</li>
 * </ul>
 *
 * @see AuthService
 * @see JwtUtil
 * @see RefreshTokenService
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final Map<String, String> resetTokens = new HashMap<>();
    private final RefreshTokenService refreshTokenService;
    private final NotificationClient notificationClient;

    /** Reusable message for user-not-found exceptions, extracted to avoid duplication (S1192). */
    private static final String USER_NOT_FOUND_MSG = "User not found";

    @Value("${app.password_reset_url}")
    private String passwordResetURL;

    /**
     * Registers a new user account after validating that the email and phone number
     * are not already in use.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Check email uniqueness (case-insensitive); throw
     *       {@link DuplicateResourceException} if taken.</li>
     *   <li>Check phone uniqueness; throw {@link DuplicateResourceException} if taken.</li>
     *   <li>BCrypt-encode the password and persist the new {@link User} entity with
     *       status {@code LOGGED_OUT}.</li>
     *   <li>Return a summary DTO of the created user.</li>
     * </ol>
     *
     * <p>Annotated with {@code @Auditable} so a {@code REGISTER} audit entry is
     * recorded automatically on success.</p>
     *
     * @param request the validated registration payload
     * @return a {@link RegisterResponseDTO} with the new user's UUID, name, email,
     *         phone, and role
     * @throws DuplicateResourceException if the email or phone is already registered
     */
    @Override
    @Auditable(action = ActionType.REGISTER, entityName = EntityType.USER)
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByPhone(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone number already registered: " + request.getPhoneNumber());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhoneNumber())
                .role(Role.valueOf(request.getRole().toUpperCase()))
                .status(LoginStatus.LOGGED_OUT)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        sendSimpleNotification(savedUser.getUserId(),
            "Welcome to FundTrack, " + savedUser.getName() + "! Your account has been successfully created.");

        return RegisterResponseDTO.builder()
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .build();
    }

    /**
     * Authenticates the user's credentials and issues a JWT access token plus a
     * refresh token.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Look up the user by email; throw {@link UserNotFoundException} if absent.</li>
     *   <li>Delegate credential verification to the {@link AuthenticationManager},
     *       which will throw if the password does not match.</li>
     *   <li>Generate a signed JWT access token containing the user's email, role,
     *       and UUID.</li>
     *   <li>Create (or replace) a refresh token via {@link RefreshTokenService}.</li>
     *   <li>Update the user's {@code LoginStatus} to {@code LOGGED_IN}.</li>
     *   <li>Return a response DTO with both tokens and profile information.</li>
     * </ol>
     *
     * <p>Annotated with {@code @Auditable} so a {@code LOGIN} audit entry is
     * recorded automatically on success.</p>
     *
     * @param request the validated login payload containing email and plaintext password
     * @return a {@link LoginResponseDTO} containing the access token, refresh token,
     *         and user profile data
     * @throws UserNotFoundException if no user exists with the provided email
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the supplied password is incorrect
     */
    @Override
    @Auditable(action = ActionType.LOGIN, entityName = EntityType.USER)
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG));

        // 1. Authenticate the user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Pass the userId to the JWT generator so the Gateway can extract it for auditing
        String accessToken = jwtUtil.generate(user.getEmail(), user.getRole().name(), user.getUserId());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        user.setStatus(LoginStatus.LOGGED_IN);
        userRepository.save(user);

        sendSimpleNotification(user.getUserId(),
            "Login successful. Welcome back, " + user.getName() + "! You are now signed in to FundTrack.");

        return LoginResponseDTO.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Logs out the user by deleting their refresh token(s) and setting their
     * {@code LoginStatus} to {@code LOGGED_OUT}.
     *
     * <p>Annotated with {@code @Auditable} so a {@code LOGOUT} audit entry is
     * recorded automatically on success.</p>
     *
     * @param request the validated logout payload containing the user's email
     * @return a {@link LogoutResponseDTO} with the user's email and UUID
     * @throws UserNotFoundException if no user exists with the provided email
     */
    @Override
    @Auditable(action = ActionType.LOGOUT, entityName = EntityType.USER)
    public LogoutResponseDTO logout(LogoutRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG));

        refreshTokenService.deleteByUser(user);
        user.setStatus(LoginStatus.LOGGED_OUT);
        userRepository.save(user);

        sendSimpleNotification(user.getUserId(),
            "You have been successfully logged out of FundTrack. See you next time!");

        return LogoutResponseDTO.builder().email(user.getEmail()).userId(user.getUserId()).build();
    }

    /**
     * Generates a one-time password-reset token for the user identified by the
     * given email and returns a reset link.
     *
     * <p>The token is a random UUID string stored in an in-memory map keyed by
     * token value with the associated email as the value. The reset URL is
     * constructed by substituting {@code {token}} in the
     * {@code app.password_reset_url} configuration property.</p>
     *
     * <p>This method is intentionally not annotated with {@code @Auditable} because
     * a successful call does not yet modify any persistent state — the audit entry
     * is created when {@link #resetPassword(String, String)} is called.</p>
     *
     * @param email the registered email address of the user requesting a reset
     * @return a {@link ForgotPasswordResponseDTO} containing the user's UUID and
     *         the fully formed reset link
     * @throws UserNotFoundException if no user exists with the provided email
     */
    @Override
    public ForgotPasswordResponseDTO forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG));
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, email);
        return ForgotPasswordResponseDTO.builder()
                .userId(user.getUserId())
                .resetLink(passwordResetURL.replace("{token}", token))
                .build();
    }

    /**
     * Validates the one-time reset token, BCrypt-encodes the new password, and
     * persists it. The token is removed from the in-memory store after use to
     * prevent replay attacks.
     *
     * <p>Annotated with {@code @Auditable} so a {@code RESET_PASSWORD} audit entry
     * is recorded automatically on success. The method returns the user's UUID so
     * the AOP aspect can capture the entity ID.</p>
     *
     * @param token       the one-time reset token from the password-reset link
     * @param newPassword the desired new plaintext password
     * @return the {@link UUID} of the user whose password was updated
     * @throws InvalidTokenException if the token is not present in the in-memory store
     * @throws UserNotFoundException if the email associated with the token no longer
     *                               maps to an existing user
     */
    @Override
    @Auditable(action = ActionType.RESET_PASSWORD, entityName = EntityType.USER)
    public UUID resetPassword(String token, String newPassword) {
        String email = resetTokens.get(token);
        if (email == null) throw new InvalidTokenException("Invalid token");
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokens.remove(token);

        sendSimpleNotification(user.getUserId(),
            "Security Alert: Your FundTrack password has been successfully reset. If you did not request this, contact support immediately.");

        return user.getUserId();
    }

    private void sendSimpleNotification(UUID userId, String message) {
        try {
            SimpleNotificationRequestDTO notification = new SimpleNotificationRequestDTO();
            notification.setUserId(userId);
            notification.setMessage(message);
            notificationClient.sendSimpleNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }
}
