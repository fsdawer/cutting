package beauty.beauty.auth.service;

import beauty.beauty.auth.dto.EmailRequest;
import beauty.beauty.auth.dto.LoginRequest;
import beauty.beauty.auth.dto.RegisterRequest;
import beauty.beauty.auth.dto.TokenResponse;

public interface AuthService {

    /** 회원가입 - 사용자 저장 후 이메일 인증 메일 발송 */
    void register(RegisterRequest request);

    /** 로그인 - 아이디/비밀번호 검증 후 JWT 발급 */
    TokenResponse login(LoginRequest request);

    /** 이메일 인증 코드 발송 */
    void sendVerifyEmail(EmailRequest request);

    /** 이메일 인증 토큰 확인 → isVerified = true */
    void verifyEmail(String token);

    /** Refresh Token으로 새로운 Access Token 발급 */
    TokenResponse refresh(String refreshToken);

    /** 로그아웃 (클라이언트 토큰 삭제 안내용; 서버 무효화는 선택) */
    void logout(Long userId);
}
