package beauty.beauty.chat.service;

import beauty.beauty.chat.dto.ChatRoomResponse;
import beauty.beauty.chat.dto.MessageResponse;
import beauty.beauty.chat.dto.SendMessageRequest;
import beauty.beauty.chat.entity.ChatRoom;
import beauty.beauty.chat.entity.Message;
import beauty.beauty.chat.repository.ChatRoomRepository;
import beauty.beauty.chat.repository.MessageRepository;
import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final StylistProfileRepository stylistProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 예약 확정(CONFIRMED) 시 자동 호출 — 이미 채팅방이 있으면 재사용, 없으면 새로 생성
    @Override
    @Transactional
    public ChatRoom createRoomForReservation(Reservation reservation) {
        return chatRoomRepository.findByReservationId(reservation.getId())
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.builder()
                                .reservation(reservation)
                                .user(reservation.getUser())
                                .stylistUser(reservation.getStylistProfile().getUser())
                                .build()
                ));
    }

    // 채팅방 단건 조회 — ChatView 진입 시 헤더 정보 로드용
    @Override
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(Long userId, Long roomId) {
        ChatRoom room = findRoomOrThrow(roomId);
        checkAccess(userId, room);
        return ChatRoomResponse.from(room, getSalonName(room.getStylistUser().getId()));
    }


    // 내가 속한 채팅방 목록 — 마지막 메시지 최신순 정렬
    // N+1 해결: 미용사 salonName을 건당 조회하지 않고 IN 절로 한 번에 가져옴
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(Long userId) {
        List<ChatRoom> asUser    = chatRoomRepository.findByUserIdOrderByLastMessageAtDesc(userId);
        List<ChatRoom> asStylist = chatRoomRepository.findByStylistUserIdOrderByLastMessageAtDesc(userId);

        List<ChatRoom> all = Stream.concat(asUser.stream(), asStylist.stream())
                .sorted(Comparator.comparing(ChatRoom::getLastMessageAt).reversed())
                .toList();

        if (all.isEmpty()) return List.of();

        // 미용사 userId 목록으로 (userId, salonName) 쌍을 단 1회 쿼리로 조회
        Set<Long> stylistUserIds = all.stream()
                .map(r -> r.getStylistUser().getId())
                .collect(Collectors.toSet());

        Map<Long, String> salonNameMap = stylistProfileRepository
                .findSalonNamesByUserIds(stylistUserIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long)   row[0],
                        row -> (String) row[1],
                        (a, b) -> a
                ));

        return all.stream()
                .map(room -> ChatRoomResponse.from(room, salonNameMap.get(room.getStylistUser().getId())))
                .toList();
    }


    // 채팅방의 메시지 내역을 오래된 순으로 반환 — 접근 권한 없으면 예외
    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long userId, Long roomId) {
        ChatRoom room = findRoomOrThrow(roomId);
        checkAccess(userId, room);
        return messageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId)
                .stream().map(MessageResponse::from).toList();

    }


    // 메시지 저장 후 STOMP 브로커로 /topic/chat/{roomId} 구독자 전체에게 브로드캐스트
    // lastMessageAt, lastMessageContent 갱신 → 채팅방 목록 최신순 반영
    @Override
    @Transactional
    public MessageResponse sendMessage(Long userId, SendMessageRequest request) {
        ChatRoom room = findRoomOrThrow(request.getRoomId());
        checkAccess(userId, room);

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Message saved = messageRepository.save(Message.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .build());

        room.setLastMessageAt(LocalDateTime.now());
        room.setLastMessageContent(request.getContent() != null ? request.getContent() : "[사진]");

        MessageResponse response = MessageResponse.from(saved);
        messagingTemplate.convertAndSend("/topic/chat/" + request.getRoomId(), response);
        return response;
    }

    // 상대방 메시지 전체 읽음 처리 — 개별 dirty checking 대신 UPDATE 단건 쿼리로 처리
    @Override
    @Transactional
    public void markAsRead(Long userId, Long roomId) {
        ChatRoom room = findRoomOrThrow(roomId);
        checkAccess(userId, room);
        messageRepository.markMessagesAsRead(roomId, userId);
    }

    // stylistUser의 userId로 StylistProfile을 조회해 salonName 반환 (단건 조회용)
    private String getSalonName(Long stylistUserId) {
        return stylistProfileRepository.findByUserId(stylistUserId)
                .map(p -> p.getSalon() != null ? p.getSalon().getName() : null)
                .orElse(null);
    }

    // roomId로 채팅방 조회 — 없으면 예외 발생
    private ChatRoom findRoomOrThrow(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
    }

    // userId가 해당 채팅방의 고객 또는 미용사인지 확인 — 아니면 예외 발생
    private void checkAccess(Long userId, ChatRoom room) {
        boolean isUser    = room.getUser().getId().equals(userId);
        boolean isStylist = room.getStylistUser().getId().equals(userId);
        if (!isUser && !isStylist) {
            throw new IllegalArgumentException("채팅방에 접근 권한이 없습니다.");
        }
    }
}
