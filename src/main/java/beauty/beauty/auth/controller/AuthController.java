package beauty.beauty.auth.controller;

import beauty.beauty.auth.dto.LoginRequest;
import beauty.beauty.auth.dto.RegisterRequest;
import beauty.beauty.auth.dto.TokenResponse;
import beauty.beauty.auth.service.AuthService;
import beauty.beauty.global.annotation.LoginUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @LoginUserId Long userId,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        authService.logout(userId, token);
        return ResponseEntity.ok().build();
    }
}
