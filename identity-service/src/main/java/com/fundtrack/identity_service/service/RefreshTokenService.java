package com.fundtrack.identity_service.service;
import com.fundtrack.identity_service.model.RefreshToken;
import com.fundtrack.identity_service.model.User;

import java.util.Optional;


public interface RefreshTokenService {

    RefreshToken createRefreshToken(String email);

    RefreshToken verifyExpiration(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}


