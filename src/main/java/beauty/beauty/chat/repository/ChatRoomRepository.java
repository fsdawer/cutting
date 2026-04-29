package beauty.beauty.chat.repository;

import beauty.beauty.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByReservationId(Long reservationId);
    List<ChatRoom> findByUserIdOrderByLastMessageAtDesc(Long userId);
    List<ChatRoom> findByStylistUserIdOrderByLastMessageAtDesc(Long stylistUserId);
}
