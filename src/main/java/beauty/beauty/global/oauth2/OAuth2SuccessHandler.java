package beauty.beauty.global.oauth2;

import beauty.beauty.global.jwt.JwtUtil;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 핸들러.
 *
 * 흐름:
 * 1. 카카오 providerId로 DB에서 유저 조회
 * 2. JWT Access Token 발급
 * 3. 프론트엔드 콜백 URL에 token 쿼리 파라미터로 리다이렉트
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // 로그인 성공 후 프론트로 리다이렉트할 URL
    private static final String REDIRECT_URL = "http://localhost:5173/oauth2/callback";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 카카오 providerId로 DB에서 유저 조회
        String providerId = String.valueOf(oAuth2User.getAttributes().get("id"));
        User user = userRepository.findByProviderAndProviderId(User.Provider.KAKAO, providerId)
                .orElseThrow(() -> new IllegalStateException("OAuth2 유저를 찾을 수 없습니다."));

        // JWT 발급
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());

        // 프론트로 토큰과 함께 리다이렉트
        // 예: http://localhost:5173/oauth2/callback?token=eyJhbGci...
        getRedirectStrategy().sendRedirect(request, response,
                REDIRECT_URL + "?token=" + accessToken);
    }
}
