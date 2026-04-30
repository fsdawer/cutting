package beauty.beauty.payment.repository;

import beauty.beauty.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByReservationId(Long reservationId);

    // 결제 내역 조회 시 연관 엔티티를 한 번에 로드 (N+1 방지)
    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.reservation r " +
           "JOIN FETCH r.stylistProfile sp " +
           "JOIN FETCH sp.user " +
           "LEFT JOIN FETCH sp.salon " +
           "JOIN FETCH r.service " +
           "WHERE r.id IN :reservationIds " +
           "ORDER BY p.createdAt ASC")
    List<Payment> findByReservationIdInOrderByCreatedAtAsc(@Param("reservationIds") List<Long> reservationIds);

    // 10분 이상 지난 PENDING 결제를 스케줄러에서 청소할 때 사용
    List<Payment> findByStatusAndCreatedAtBefore(Payment.PayStatus status, LocalDateTime dateTime);
}
