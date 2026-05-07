package beauty.beauty.chat.dto;

import beauty.beauty.chat.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponse {
    private Long roomId;
    private Long reservationId;
    private Long userId;
    private String userName;
    private Long stylistUserId;
    private String stylistUserName;
    private String salonName;
    private String lastMessageContent;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public static ChatRoomResponse from(ChatRoom room, String salonName) {
        return from(room, salonName, 0L);
    }

    public static ChatRoomResponse from(ChatRoom room, String salonName, long unreadCount) {
        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .reservationId(room.getReservation().getId())
                .userId(room.getUser().getId())
                .userName(room.getUser().getName())
                .stylistUserId(room.getStylistUser().getId())
                .stylistUserName(room.getStylistUser().getName())
                .salonName(salonName)
                .lastMessageContent(room.getLastMessageContent())
                .createdAt(room.getCreatedAt())
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }

    public static ChatRoomResponse from(ChatRoom room) {
        return from(room, null, 0L);
    }
}
