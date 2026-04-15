package com.cts.fundtrack.identity.service;

import java.util.UUID;

import com.cts.fundtrack.common.dto.ForgotPasswordResponseDTO;
import com.cts.fundtrack.common.dto.LoginRequestDTO;
import com.cts.fundtrack.common.dto.LoginResponseDTO;
import com.cts.fundtrack.common.dto.LogoutRequestDTO;
import com.cts.fundtrack.common.dto.LogoutResponseDTO;
import com.cts.fundtrack.common.dto.RegisterRequestDTO;
import com.cts.fundtrack.common.dto.RegisterResponseDTO;

public interface AuthService {
    RegisterResponseDTO register(RegisterRequestDTO request);
    LoginResponseDTO login(LoginRequestDTO request);
    LogoutResponseDTO logout(LogoutRequestDTO request);
    ForgotPasswordResponseDTO forgotPassword(String email);
    UUID resetPassword(String token, String newPassword);
    // Add this inside your AuthService interface
    }