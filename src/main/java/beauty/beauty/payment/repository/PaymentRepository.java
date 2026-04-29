package beauty.beauty.payment.repository;

import beauty.beauty.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByReservationId(Long reservationId);

    // 예약 ID 목록으로 결제 내역 한 번에 조회
    List<Payment> findByReservationIdInOrderByCreatedAtAsc(List<Long> reservationIds);


    // 10분 이상 지난 PENDING 결제를 스케줄러에서 청소할 때 사용
    List<Payment> findByStatusAndCreatedAtBefore(Payment.PayStatus status, LocalDateTime dateTime);
}
