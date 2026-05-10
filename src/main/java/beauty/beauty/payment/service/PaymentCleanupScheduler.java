package beauty.beauty.payment.service;

import beauty.beauty.payment.entity.Payment;
import beauty.beauty.payment.repository.PaymentRepository;
import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.reservation.repository.ReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class PaymentCleanupScheduler {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${toss.secret-key}")
    private String secretKey;

    public PaymentCleanupScheduler(
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            TransactionTemplate transactionTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.paymentRepository     = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.transactionTemplate   = transactionTemplate;
        this.objectMapper          = objectMapper;
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupExpiredPayments() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(10);
        List<Payment> expiredPayments = paymentRepository
                .findByStatusAndCreatedAtBefore(Payment.PayStatus.PENDING, expiredTime);
        if (expiredPayments.isEmpty()) return;

        for (Payment payment : expiredPayments) {
            try {
                processExpiredPayment(payment);
            } catch (Exception e) {
                // 한 건 실패해도 나머지 계속 처리, 다음 스케줄러 실행에서 재시도
                log.error("[Cleanup] 처리 실패, 다음 실행에서 재시도 — paymentId={}", payment.getId(), e);
            }
        }
    }

    private void processExpiredPayment(Payment payment) throws Exception {
        // Toss에서 실제 결제 상태 조회 (404면 null — 결제창 미진입 상태의 정상 만료)
        String tossResponse = queryTossOrder(payment.getOrderId());

        if (tossResponse != null) {
            JsonNode node = objectMapper.readTree(tossResponse);
            String tossStatus = node.path("status").asText("");
            String paymentKey = node.path("paymentKey").asText(null);

            // Toss DONE인데 DB PENDING → confirm() Flow 3 실패 불일치 건 → 보상 트랜잭션
            if ("DONE".equals(tossStatus) && paymentKey != null) {
                cancelTossPayment(paymentKey);
                log.warn("[보상 트랜잭션] DB 불일치 건 Toss 취소 완료 — orderId={}", payment.getOrderId());
            }
        }

        // DB 정리 — 건별 독립 트랜잭션 (한 건 실패가 다른 건 롤백으로 이어지지 않도록)
        transactionTemplate.execute(status -> {
            Reservation reservation = payment.getReservation();
            if (reservation != null && reservation.getStatus() == Reservation.Status.PENDING) {
                reservation.setStatus(Reservation.Status.CANCELLED);
                reservationRepository.save(reservation);
            }
            paymentRepository.delete(payment);
            return null;
        });
        log.info("[Cleanup] 만료 결제 정리 완료 — paymentId={}", payment.getId());
    }

    // Toss 주문 조회 — 404(Toss에 기록 없음)면 null 반환
    private String queryTossOrder(String orderId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tosspayments.com/v1/payments/orders/" + orderId))
                .header("Authorization", "Basic " + tossCredentials())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return null;
        if (response.statusCode() != 200) {
            throw new RuntimeException("Toss 조회 실패 [" + response.statusCode() + "]: " + response.body());
        }
        return response.body();
    }

    private void cancelTossPayment(String paymentKey) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel"))
                .header("Authorization", "Basic " + tossCredentials())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"cancelReason\":\"시스템 오류로 인한 자동 취소\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Toss 취소 실패 [" + response.statusCode() + "]: " + response.body());
        }
    }

    private String tossCredentials() {
        return Base64.getEncoder()
                .encodeToString((secretKey.trim() + ":").getBytes(StandardCharsets.UTF_8));
    }
}
