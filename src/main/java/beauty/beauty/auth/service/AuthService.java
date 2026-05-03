package beauty.beauty.auth.service;

import beauty.beauty.auth.dto.LoginRequest;
import beauty.beauty.auth.dto.RegisterRequest;
import beauty.beauty.auth.dto.TokenResponse;

public interface AuthService {
    void register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
    void logout(Long userId, String accessToken);
}
