package beauty.beauty.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * ┌─────────────────────────────────────────────────────────────┐
 *  WebSocket / STOMP 설정
 *  ─────────────────────────────────────────────────────────────
 *  WebSocket: HTTP와 달리 한 번 연결하면 양방향 통신이 지속되는 프로토콜.
 *             서버가 클라이언트에게 먼저 데이터를 보낼 수 있다 → 실시간 채팅에 사용.
 *
 *  STOMP (Simple Text Oriented Messaging Protocol):
 *  WebSocket 위에서 동작하는 메시징 프로토콜.
 *  채팅방 개념(구독/발행)을 쉽게 구현할 수 있게 해준다.
 *
 *  동작 흐름:
 *  [클라이언트A] ──발행──→ /app/chat/{roomId} ──→ @MessageMapping 처리
 *                                                       │
 *                                              /topic/chat/{roomId} 로 전송
 *                                                       │
 *  [클라이언트B] ←──수신── /topic/chat/{roomId} 구독 ←─┘
 *
 *  채팅방에서:
 *  - 클라이언트는 /ws 엔드포인트로 WebSocket 연결
 *  - 채팅방에 들어오면 /topic/chat/{roomId} 구독
 *  - 메시지 보낼 때 /app/chat/{roomId} 로 발행
 * └─────────────────────────────────────────────────────────────┘
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompChannelInterceptor stompChannelInterceptor;

    /**
     * 메시지 브로커 설정.
     *
     * enableSimpleBroker("/topic"):
     *   "/topic/**" 경로를 구독하는 클라이언트에게 메시지를 자동으로 전달하는
     *   인메모리 브로커를 활성화한다. (Redis나 RabbitMQ 같은 외부 브로커 없이도 동작)
     *
     * setApplicationDestinationPrefixes("/app"):
     *   클라이언트가 /app/** 으로 메시지를 보내면 @MessageMapping 메서드가 받는다.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");        // 구독 경로 prefix
        registry.setApplicationDestinationPrefixes("/app"); // 발행 경로 prefix
    }

    /**
     * WebSocket 연결 엔드포인트 등록.
     *
     * /ws: 클라이언트가 최초 WebSocket 연결을 맺는 URL.
     *      예) Vue에서: new SockJS('http://localhost:8080/ws')
     *
     * withSockJS(): WebSocket을 지원하지 않는 환경에서
     *              HTTP Polling 등으로 폴백(fallback)해주는 라이브러리.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 허용
                .withSockJS();
    }

    // STOMP CONNECT 시 JWT 검증 인터셉터 등록
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
    }
}
