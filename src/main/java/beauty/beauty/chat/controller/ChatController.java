package beauty.beauty.chat.controller;

import beauty.beauty.chat.dto.ChatRoomResponse;
import beauty.beauty.chat.dto.MessageResponse;
import beauty.beauty.chat.dto.SendMessageRequest;
import beauty.beauty.chat.service.ChatService;
import beauty.beauty.global.annotation.LoginUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;


    // 내 채팅창 목록
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(@LoginUserId Long userId) {
        return ResponseEntity.ok(chatService.getMyRooms(userId));
    }

    // 채팅방 단건 조회 (ChatView 헤더 정보용)
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomResponse> getRoom(
            @LoginUserId Long userId,
            @PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getRoom(userId, roomId));
    }



    // 메시지 내역
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @LoginUserId Long userId,
            @PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getMessages(userId, roomId));
    }


    // 읽음 처리
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(@LoginUserId Long userId, @PathVariable Long roomId) {
        chatService.markAsRead(userId, roomId);
        return ResponseEntity.ok().build();
    }


    // WebSocket STOMP: /app/chat/{roomId} → /topic/chat/{roomId} 브로드캐스트
    // Principal은 StompChannelInterceptor가 CONNECT 시 JWT로 설정 — senderId 위조 불가
    @MessageMapping("/chat/{roomId}")
    public void handleMessage(@DestinationVariable Long roomId,
                              SendMessageRequest request,
                              Principal principal) {
        if (principal == null) return; // 미인증 연결 무시
        Long userId = Long.parseLong(principal.getName());
        request.setRoomId(roomId);
        chatService.sendMessage(userId, request);
    }


}
