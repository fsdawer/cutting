package beauty.beauty.chat.repository;

import beauty.beauty.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByReservationId(Long reservationId);
    List<ChatRoom> findByReservationIdIn(List<Long> reservationIds);

    @Query("SELECT r FROM ChatRoom r JOIN FETCH r.stylistUser WHERE r.user.id = :userId ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);

    @Query("SELECT r FROM ChatRoom r JOIN FETCH r.stylistUser WHERE r.stylistUser.id = :stylistUserId ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findByStylistUserIdOrderByLastMessageAtDesc(@Param("stylistUserId") Long stylistUserId);
}
