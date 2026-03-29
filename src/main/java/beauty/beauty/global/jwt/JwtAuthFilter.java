package beauty.beauty.global.jwt;

import beauty.beauty.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ┌─────────────────────────────────────────────────────────────┐
 *  JWT 인증 필터
 *  ─────────────────────────────────────────────────────────────
 *  모든 HTTP 요청을 가로채서 JWT 토큰을 검사한다.
 *
 *  흐름:
 *  [요청] → JwtAuthFilter → 토큰 유효? → SecurityContext에 인증 정보 저장 → [Controller]
 *
 *  OncePerRequestFilter: 요청당 딱 한 번만 실행되도록 보장하는 Spring 필터.
 *
 *  클라이언트는 요청 시 헤더에 이렇게 보내야 한다:
 *  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 * └─────────────────────────────────────────────────────────────┘
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 "Bearer {token}" 추출
        String token = resolveToken(request);

        // 2. 토큰이 존재하고 유효한 경우에만 인증 처리
        if (token != null && jwtUtil.isValid(token)) {

            // 3. 토큰에서 userId를 꺼내 DB에서 실제 사용자 조회
            Long userId = jwtUtil.getUserId(token);

            userRepository.findById(userId).ifPresent(user -> {
                // 4. Spring Security 인증 객체 생성
                //    - principal: userId 문자열 → authentication.getName()으로 꺼낼 수 있음
                //    - credentials: null
                //    - authorities: "ROLE_USER" 또는 "ROLE_STYLIST"
                var auth = new UsernamePasswordAuthenticationToken(
                        String.valueOf(userId), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));

                // 5. SecurityContext에 저장 → 이후 요청 처리에서 인증된 사용자로 인식
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        // 6. 다음 필터로 넘김 (토큰이 없으면 그냥 통과 → 보호된 API는 SecurityConfig에서 막힘)
        filterChain.doFilter(request, response);
    }

    /**
     * "Authorization: Bearer {token}" 헤더에서 토큰 문자열만 추출.
     * 헤더가 없거나 형식이 맞지 않으면 null 반환.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7); // "Bearer " 이후 토큰만 반환
        }
        return null;
    }
}
