package beauty.beauty.global.config;

import beauty.beauty.global.jwt.JwtUtil;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP CONNECT 프레임에서 Authorization 헤더를 읽어 JWT를 검증하고
 * WebSocket 세션의 Principal(인증 주체)을 설정한다.
 *
 * 이렇게 하면 @MessageMapping 핸들러에서 Principal로 userId를 꺼낼 수 있어
 * 클라이언트가 senderId를 임의로 위조하는 보안 취약점을 제거할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearer = accessor.getFirstNativeHeader("Authorization");
            if (bearer != null && bearer.startsWith("Bearer ")) {
                String token = bearer.substring(7);
                if (jwtUtil.isValid(token)) {
                    Long userId = jwtUtil.getUserId(token);
                    userRepository.findById(userId).ifPresent(user -> {
                        var auth = new UsernamePasswordAuthenticationToken(
                                String.valueOf(userId), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                        );
                        // WebSocket 세션에 인증 주체 등록 → @MessageMapping에서 Principal로 사용
                        accessor.setUser(auth);
                    });
                }
            }
        }
        return message;
    }
}
