package beauty.beauty.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatController {
    // REST
    // GET  /api/chat/rooms             내 채팅방 목록
    // GET  /api/chat/rooms/{roomId}/messages  메시지 내역

    // WebSocket (STOMP)
    // @MessageMapping("/chat/{roomId}")         → 메시지 수신
    // @SendTo("/topic/chat/{roomId}")           → 메시지 브로드캐스트
}
