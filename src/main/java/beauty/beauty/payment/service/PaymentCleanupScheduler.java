package beauty.beauty.payment.service;

import beauty.beauty.payment.entity.Payment;
import beauty.beauty.payment.repository.PaymentRepository;
import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCleanupScheduler {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    @Scheduled(fixedDelay = 60000)  // 1분마다 실행
    @Transactional
    public void cleanupExpiredPayments() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(10);

        // 10분이 지나도 PENDING인 결제 조회
        List<Payment> expiredPayments = paymentRepository
                .findByStatusAndCreatedAtBefore(Payment.PayStatus.PENDING, expiredTime);

        if (expiredPayments.isEmpty()) return;

        // 연관된 예약도 CANCELLED 처리 (결제 미완료 예약이 슬롯을 계속 점유하지 않도록)
        List<Reservation> reservationsToCancel = expiredPayments.stream()
                .map(Payment::getReservation)
                .filter(r -> r != null && r.getStatus() == Reservation.Status.PENDING)
                .toList();

        if (!reservationsToCancel.isEmpty()) {
            reservationsToCancel.forEach(r -> r.setStatus(Reservation.Status.CANCELLED));
            reservationRepository.saveAll(reservationsToCancel);
            log.info("만료된 PENDING 예약 {}건 취소 처리", reservationsToCancel.size());
        }

        paymentRepository.deleteAll(expiredPayments);
        log.info("만료된 PENDING 결제 {}건 삭제 처리", expiredPayments.size());
    }
}
