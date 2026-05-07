package beauty.beauty.chat.entity;

import beauty.beauty.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    // 채팅방 메시지 목록: WHERE room_id=? ORDER BY created_at ASC
    @Index(name = "idx_msg_room_created", columnList = "room_id, created_at"),
    // cursor 기반 미읽음 카운트: WHERE room_id=? AND sender_id!=? AND id>?
    @Index(name = "idx_msg_room_sender_id", columnList = "room_id, sender_id, id"),
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Message {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
