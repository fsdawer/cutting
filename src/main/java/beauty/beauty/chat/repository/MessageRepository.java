package beauty.beauty.chat.repository;

import beauty.beauty.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long roomId);
    long countByChatRoomIdAndIsReadFalseAndSenderIdNot(Long roomId, Long senderId);
}
