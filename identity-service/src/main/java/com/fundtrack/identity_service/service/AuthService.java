package com.fundtrack.identity_service.service;

import com.fundtrack.identity_service.dto.logindto.LoginRequestDTO;
import com.fundtrack.identity_service.dto.logindto.LoginResponseDTO;
import com.fundtrack.identity_service.dto.logoutdto.LogoutRequestDTO;
import com.fundtrack.identity_service.dto.logoutdto.LogoutResponseDTO;
import com.fundtrack.identity_service.dto.passwordresetdto.ForgotPasswordResponseDTO;
import com.fundtrack.identity_service.dto.registerdto.RegisterRequestDTO;
import com.fundtrack.identity_service.dto.registerdto.RegisterResponseDTO;

public interface AuthService {

    RegisterResponseDTO register(RegisterRequestDTO request);

    LoginResponseDTO login(LoginRequestDTO request);

    LogoutResponseDTO logout(LogoutRequestDTO request);

    ForgotPasswordResponseDTO forgotPassword(String email);

    void resetPassword (String token, String newPassword);


}