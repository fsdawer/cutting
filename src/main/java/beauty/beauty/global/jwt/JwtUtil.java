package beauty.beauty.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * ┌─────────────────────────────────────────────────────────────┐
 *  JWT (JSON Web Token) 란?
 *  ─────────────────────────────────────────────────────────────
 *  로그인 성공 후 서버가 사용자에게 발급하는 "디지털 신분증".
 *  클라이언트는 이 토큰을 저장해두고, 이후 요청마다 헤더에 담아 보낸다.
 *  서버는 DB 조회 없이 토큰 자체를 검증해서 누구인지 확인한다 → 빠르고 Stateless.
 *
 *  구조: header.payload.signature
 *  - header   : 알고리즘 정보 (HS256 등)
 *  - payload  : 실제 데이터 (userId, role, 만료시각 등) → Base64 인코딩, 누구나 볼 수 있음
 *  - signature: secret key로 서명 → 서버만 검증 가능, 위변조 방지
 *
 *  토큰 종류:
 *  - Access Token  : 짧은 수명 (1시간). API 요청에 사용.
 *  - Refresh Token : 긴 수명 (7일). Access Token 만료 시 재발급에 사용.
 * └─────────────────────────────────────────────────────────────┘
 */
@Component
public class JwtUtil {

    /** HMAC-SHA256 서명에 사용할 비밀 키 (application.yaml 또는 환경변수에서 주입) */
    private final SecretKey key;

    /** Access Token 만료 시간 (ms) */
    private final long accessTokenExp;

    /** Refresh Token 만료 시간 (ms) */
    private final long refreshTokenExp;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExp,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExp) {
        // 문자열 secret을 바이트 배열로 변환하여 HMAC 키 생성 (최소 256bit 필요)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExp = accessTokenExp;
        this.refreshTokenExp = refreshTokenExp;
    }

    /**
     * Access Token 생성.
     * payload 에 userId, username, role 을 담아 서명 후 반환.
     */
    public String generateAccessToken(Long userId, String username, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))       // 토큰 주체 (userId)
                .claim("username", username)            // 커스텀 클레임
                .claim("role", role)
                .issuedAt(new Date())                   // 발급 시각
                .expiration(new Date(System.currentTimeMillis() + accessTokenExp)) // 만료 시각
                .signWith(key)                          // 서명 (HMAC-SHA256)
                .compact();                             // 문자열로 직렬화
    }

    /**
     * Refresh Token 생성.
     * 민감한 정보 없이 userId 만 담는다.
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExp))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 Claims(payload) 추출.
     * 서명 검증 실패 또는 만료 시 JwtException 발생.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)          // 서명 검증
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰 유효성 검사 (서명 + 만료 여부).
     * true = 유효, false = 위변조되었거나 만료됨.
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 userId 추출.
     */
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }
}
