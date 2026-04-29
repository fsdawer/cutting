package beauty.beauty.chat.service;

import beauty.beauty.chat.dto.ChatRoomResponse;
import beauty.beauty.chat.dto.MessageResponse;
import beauty.beauty.chat.dto.SendMessageRequest;
import beauty.beauty.chat.entity.ChatRoom;
import beauty.beauty.reservation.entity.Reservation;

import java.util.List;

public interface ChatService {

    // 예약 확정 시 채팅방 자동 생성 (ReservationServiceImpl에서 호출)
    ChatRoom createRoomForReservation(Reservation reservation);

    // 채팅방 단건 조회
    ChatRoomResponse getRoom(Long userId, Long roomId);

    // 내 채팅방 목록 (고객/미용사 공용)
    List<ChatRoomResponse> getMyRooms(Long userId);

    // 메시지 내역 조회
    List<MessageResponse> getMessages(Long userId, Long roomId);

    // 메시지 전송
    MessageResponse sendMessage(Long userId, SendMessageRequest sendMessageRequest);

    // 읽음 처리
    void markAsRead(Long userId, Long roomId);
}
