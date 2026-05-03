package beauty.beauty.global.jwt;

import beauty.beauty.global.redis.TokenBlacklistService;
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

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtUtil.isValid(token) && !tokenBlacklistService.isBlacklisted(token)) {
            Long userId = jwtUtil.getUserId(token);

            userRepository.findById(userId).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        String.valueOf(userId), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
