package beauty.beauty.chat.repository;

import beauty.beauty.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
    long countByChatRoomIdAndIsReadFalseAndSenderIdNot(Long chatRoomId, Long userId);

    // 상대방 메시지 전체 읽음 처리 — 메시지 N개를 UPDATE 1회로 처리
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true " +
           "WHERE m.chatRoom.id = :roomId AND m.sender.id != :userId AND m.isRead = false")
    int markMessagesAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
