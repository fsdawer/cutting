package beauty.beauty.payment.service;

import beauty.beauty.payment.entity.Payment;
import beauty.beauty.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentCleanupScheduler {

    private final PaymentRepository paymentRepository;

    @Scheduled(fixedDelay = 60000)  // 1분마다 실행
    @Transactional
    public void cleanupExpiredPayments() {
        // 10분 이전 시각 계산
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(10);

        // 10분이 지나도 PENDING인 것들 조회
        List<Payment> expiredList = paymentRepository
                .findByStatusAndCreatedAtBefore(Payment.PayStatus.PENDING, expiredTime);

        // 전부 삭제 (다음 prepare 호출 시 새 Payment가 발급됨)
        paymentRepository.deleteAll(expiredList);
    }
}