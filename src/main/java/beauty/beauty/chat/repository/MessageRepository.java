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

    // Redis lastread 용: 방의 최신 메시지 ID 조회
    @Query("SELECT MAX(m.id) FROM Message m WHERE m.chatRoom.id = :roomId")
    Long findMaxIdByRoomId(@Param("roomId") Long roomId);

    // cursor 기반 미읽음 카운트: Redis에 lastReadId 있을 때 사용 (PK 범위 조건 → 인덱스 활용)
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.chatRoom.id = :roomId AND m.sender.id != :userId AND m.id > :lastReadId")
    long countUnreadSince(@Param("roomId") Long roomId,
                          @Param("userId") Long userId,
                          @Param("lastReadId") Long lastReadId);
}
