package beauty.beauty.auth.controller;

import beauty.beauty.auth.dto.EmailRequest;
import beauty.beauty.auth.dto.LoginRequest;
import beauty.beauty.auth.dto.RegisterRequest;
import beauty.beauty.auth.dto.TokenResponse;
import beauty.beauty.auth.service.AuthService;
import beauty.beauty.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/register — 회원가입 */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    /** POST /api/auth/login — 로그인 → JWT 반환 */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** POST /api/auth/email/send — 이메일 인증 코드 발송 */
    @PostMapping("/email/send")
    public ResponseEntity<Void> sendVerifyEmail(@Valid @RequestBody EmailRequest request) {
        authService.sendVerifyEmail(request);
        return ResponseEntity.ok().build();
    }

    /** GET /api/auth/email/verify?token=... — 이메일 인증 확인 */
    @GetMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    /** POST /api/auth/refresh — Access Token 갱신 */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    /** POST /api/auth/logout — 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        authService.logout(user.getId());
        return ResponseEntity.ok().build();
    }
}
