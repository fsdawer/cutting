package beauty.beauty.reservation.repository;

import beauty.beauty.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Reservation> findByStylistProfileId(Long stylistId);
    List<Reservation> findByUserIdAndStatus(Long userId, Reservation.Status status);
    boolean existsByStylistProfileIdAndReservedAtAndStatusIn(Long stylistId, LocalDateTime reservedAt, List<Reservation.Status> statuses);
    List<Reservation> findByStylistProfileIdAndReservedAtBetweenAndStatusIn(Long stylistId, LocalDateTime start, LocalDateTime end, List<Reservation.Status> statuses);

}
