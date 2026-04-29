package beauty.beauty.global.config;

import beauty.beauty.auth.service.CustomOAuth2UserService;
import beauty.beauty.global.jwt.JwtAuthFilter;
import beauty.beauty.global.jwt.JwtUtil;
import beauty.beauty.global.oauth2.OAuth2SuccessHandler;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정.
 *
 * 핵심 개념:
 * - 인증(Authentication): "이 사람이 누구인가?" → JWT 토큰으로 확인
 * - 인가(Authorization):  "이 사람이 이걸 할 수 있는가?" → Role(USER/STYLIST)로 제한
 *
 * 요청 처리 흐름:
 * [요청] → CORS 필터 → JwtAuthFilter(토큰검증) → SecurityFilterChain(권한 체크) → [Controller]
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 비활성화: REST API + JWT 방식은 세션을 사용하지 않으므로 CSRF 공격에 해당 없음
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정 적용 (아래 corsConfigurationSource() 빈 사용)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 미사용 (Stateless): JWT를 쓰기 때문에 서버가 세션을 저장할 필요 없음
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // API별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 로그인/회원가입/이메일인증은 누구나 접근 가능
                        .requestMatchers("/api/auth/**").permitAll()

                        // 미용사 목록·상세 조회는 비로그인도 가능
                        .requestMatchers(HttpMethod.GET, "/api/stylists", "/api/stylists/**").permitAll()

                        // 리뷰 조회도 공개
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()

                        // 결제 실패 시 PENDING 즉시 취소 — 리다이렉트 페이지에서 토큰 없이 호출
                        .requestMatchers(HttpMethod.POST, "/api/payments/cancel-pending").permitAll()

                        // OAuth2 콜백 경로 허용
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()

                        // WebSocket SockJS 핸드셰이크 허용
                        // (SockJS는 HTTP /ws/info GET을 먼저 보내는데, 여기에 Authorization 헤더가 없어서 막힘)
                        // 실제 메시지 수준 인증은 senderId 검증으로 대체
                        .requestMatchers("/ws/**").permitAll()

                        // 나머지 모든 요청은 로그인(JWT 토큰) 필요
                        .anyRequest().authenticated()
                )

                // 소셜 로그인 (카카오)
                // CustomOAuth2UserService: 유저 정보 조회 및 자동 회원가입
                // OAuth2SuccessHandler: JWT 발급 후 프론트로 리다이렉트
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // UsernamePasswordAuthenticationFilter 앞에 JWT 필터 삽입
                // → 모든 요청에서 JWT를 먼저 검사하게 됨
                .addFilterBefore(new JwtAuthFilter(jwtUtil, userRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 비밀번호 암호화기. BCrypt 알고리즘 사용.
     * 회원가입 시 저장, 로그인 시 검증에 사용.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정.
     * CORS: 브라우저 보안 정책 — 다른 도메인(Origin) 간 요청을 기본적으로 차단한다.
     * Vue(5173) → Spring(8080) 요청이 이에 해당하므로 명시적으로 허용해야 함.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173")); // Vue 개발 서버
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 쿠키/인증 헤더 포함 요청 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
