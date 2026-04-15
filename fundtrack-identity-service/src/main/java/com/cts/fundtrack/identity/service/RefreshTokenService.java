package com.cts.fundtrack.identity.service;

import com.cts.fundtrack.identity.model.RefreshToken;
import com.cts.fundtrack.identity.model.User;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String email);
    RefreshToken verifyExpiration(RefreshToken token);
    Optional<RefreshToken> findByToken(String string);
    UUID deleteByUser(User user);
}