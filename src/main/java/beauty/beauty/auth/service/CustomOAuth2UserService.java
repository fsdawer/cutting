package beauty.beauty.auth.service;

import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * 카카오 OAuth2 로그인 시 유저 정보를 처리하는 서비스.
 *
 * 흐름:
 * 1. 카카오에서 Access Token으로 유저 정보(닉네임, 이메일) 조회
 * 2. DB에 해당 카카오 유저가 있는지 확인
 * 3. 없으면 자동 회원가입 (Provider=KAKAO)
 * 4. 있으면 그냥 반환
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 카카오 API에서 유저 정보 조회
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 카카오 응답 구조: { id, kakao_account: { email, profile: { nickname } } }
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = String.valueOf(attributes.get("id"));

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile     = (Map<String, Object>) kakaoAccount.get("profile");

        String nickname = (String) profile.get("nickname");
        String email    = kakaoAccount.containsKey("email")
                          ? (String) kakaoAccount.get("email")
                          : "kakao_" + providerId + "@kakao.com"; // 이메일 미제공 시 대체값

        // DB에 카카오 유저 있는지 확인 → 없으면 자동 회원가입
        userRepository.findByProviderAndProviderId(User.Provider.KAKAO, providerId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username("kakao_" + providerId)   // 고유한 username
                                .email(email)
                                .password(null)                    // 소셜 로그인은 비밀번호 없음
                                .name(nickname)
                                .role(User.Role.USER)
                                .provider(User.Provider.KAKAO)
                                .providerId(providerId)
                                .isVerified(true)                  // 소셜 로그인은 이메일 인증 불필요
                                .build()
                ));

        // Spring Security가 인식할 수 있는 형태로 반환
        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id"  // 카카오 응답의 기본 키
        );
    }
}
