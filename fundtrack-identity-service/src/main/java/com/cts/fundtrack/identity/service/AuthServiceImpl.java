package com.cts.fundtrack.identity.service;

import com.cts.fundtrack.common.aspect.Auditable;
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

    @Value("${app.password_reset_url}")
    private String passwordResetURL;

    @Override
    @Auditable(action = ActionType.REGISTER, entityName = EntityType.USER)
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhoneNumber())
                .role(Role.valueOf(request.getRole().toUpperCase()))
                .status(LoginStatus.LOGGED_OUT)
                .build();

        User savedUser = userRepository.saveAndFlush(user);
        return RegisterResponseDTO.builder()
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .build();
    }

    @Override
    @Auditable(action = ActionType.LOGIN, entityName = EntityType.USER)
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 1. Authenticate the user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. UPDATED: Pass the userId to the JWT generator
        // This ensures the Gateway can extract the ID for auditing in other services
        String accessToken = jwtUtil.generate(user.getEmail(), user.getRole().name(), user.getUserId());
        
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        user.setStatus(LoginStatus.LOGGED_IN);
        userRepository.save(user);

        return LoginResponseDTO.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Auditable(action = ActionType.LOGOUT, entityName = EntityType.USER)
    public LogoutResponseDTO logout(LogoutRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        refreshTokenService.deleteByUser(user);
        user.setStatus(LoginStatus.LOGGED_OUT);
        userRepository.save(user);

        return LogoutResponseDTO.builder().email(user.getEmail()).userId(user.getUserId()).build();
    }

    @Override
    public ForgotPasswordResponseDTO forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, email);
        return ForgotPasswordResponseDTO.builder()
                .userId(user.getUserId())
                .resetLink(passwordResetURL.replace("{token}", token))
                .build();
    }

    @Override
    @Auditable(action = ActionType.RESET_PASSWORD, entityName = EntityType.USER)
    public UUID resetPassword(String token, String newPassword) {
        String email = resetTokens.get(token);
        if (email == null) throw new InvalidTokenException("Invalid token");
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokens.remove(token);
        return user.getUserId();
    }
}