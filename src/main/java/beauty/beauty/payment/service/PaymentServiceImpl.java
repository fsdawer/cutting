package beauty.beauty.payment.service;

import beauty.beauty.chat.service.ChatService;
import beauty.beauty.payment.dto.*;
import beauty.beauty.payment.entity.Payment;
import beauty.beauty.payment.repository.PaymentRepository;
import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.reservation.repository.ReservationRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Value("${toss.secret-key}")
    private String secretKey;

    // HttpClient는 스레드 안전하므로 재사용 (매 API 호출마다 새로 생성하면 성능 낭비)
    private final HttpClient httpClient = HttpClient.newHttpClient();


    // 결제 준비 — orderId 발급 + Payment(PENDING) DB 저장
    @Override
    @Transactional
    public PaymentPrepareResponse prepare(Long userId, PaymentPrepareRequest request) {

        // 1. userId로 User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 아닙니다"));

        // 2. reservationId로 예약정보 조회
        // PaymentPrepareRequest request 안에는 예약 ID가 들어있음
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("해당 예약은 없습니다."));

        // 해당 예약이 내 예약인지 검증 (유저 조회에서 조회된 아이디가 예약 테이블 안에있는 아이디와 같은지)
        if(!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예약만 결제할 수 있습니다.");
        }

        // 예약 ID로 기존 결제 내역이 있는지 조회
        // 파라미터는 예약 테이블에 저장된 예약 아이디를 가져와야하기 때문에 reservation.getId
        // request에 있는 예약 ID는 요청에 포함된 ID이기 때문임
        paymentRepository.findByReservationId(reservation.getId())
                .ifPresent(existingPayment -> {
                            // 이미 결제 완료(PAID) 상태라면 예외 발생
                            if (existingPayment.getStatus() == Payment.PayStatus.PAID) {
                                throw new IllegalStateException("이미 결제가 완료된 예약입니다.");
                            }

                            // 환불된 건에 대해서도 재결제를 막고 싶다면 추가
                            if(existingPayment.getStatus() == Payment.PayStatus.REFUNDED) {
                                throw new IllegalStateException("이미 환불 처리된 예약입니다.");
                            }

                    // 결제창을 열었다가 닫아서 PENDING 상태로 남아있는 경우, 이전 내역은 삭제
                    if (existingPayment.getStatus() == Payment.PayStatus.PENDING) {
                        paymentRepository.delete(existingPayment);
                        paymentRepository.flush(); // 즉시 쿼리를 날려서 새 Payment 저장 시 유니크 제약조건(예약 1개당 Payment 1개) 충돌을 방지합니다.
                    }
                });

        // UUID로 고유한 orderId 생성 (UUID.randomUUID().toString())
        String orderId = UUID.randomUUID().toString();

        // Payment 엔티티 생성
        Payment payment = Payment.builder()
                .reservation(reservation) // 예약 정보
                .orderId(orderId)         // 주문 번호
                .amount(reservation.getTotalPrice()) // 총 가격
                .status(Payment.PayStatus.PENDING)
                .method(Payment.Method.TOSS) // 임시 기본값, confirm에서 실제 수단(카드, 카카오페이 등)으로 덮어씀
                .build();

        //paymentRepository.save()
        Payment savedPayment = paymentRepository.save(payment);

        // PaymentPrepareResponse 반환
        return PaymentPrepareResponse.builder()
                .paymentId(savedPayment.getId())
                .orderId(savedPayment.getOrderId())
                .amount(savedPayment.getAmount())
                .build();
    }


    // 결제 승인 — 토스페이먼츠 서버에 최종 승인 요청 후 PAID 처리
    @Override
    @Transactional
    public PaymentResponse confirm(Long userId, PaymentConfirmRequest request) {
        // 1. orderId로 Payment 조회
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // 2. 본인 결제인지 검증
        if(!payment.getReservation().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 결제만 처리할 수 있습니다.");
        }

        // 3. 이미 처리된 결제인지 확인
        if(payment.getStatus() == Payment.PayStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 건입니다.");
        }

        // 4. 금액 위변조 검증
        if(payment.getAmount() != request.getAmount()) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        // 5. 토스페이먼츠 서버에 최종 승인 요청
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((secretKey.trim() + ":").getBytes(StandardCharsets.UTF_8));
            String body = String.format(
                    "{\"paymentKey\":\"%s\",\"orderId\":\"%s\",\"amount\":%d}",
                    request.getPaymentKey(), request.getOrderId(), request.getAmount()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tosspayments.com/v1/payments/confirm"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("토스페이먼츠 승인 실패 [{}]: {}", response.statusCode(), response.body());
                throw new IllegalStateException("토스페이먼츠 승인 실패: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("토스페이먼츠 통신 중 오류 발생", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("토스페이먼츠 API 호출 중 오류가 발생했습니다.", e);
        }

        // 6. DB 업데이트 — 결제 상태 PAID + 연관 예약 상태 CONFIRMED 동시 변경
        payment.setStatus(Payment.PayStatus.PAID);
        payment.setPaymentKey(request.getPaymentKey());
        payment.setPaidAt(LocalDateTime.now());

        Reservation reservation = payment.getReservation();
        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservationRepository.save(reservation);

        // 결제 확정 즉시 채팅방 생성 — 고객과 미용사가 바로 소통 가능
        chatService.createRoomForReservation(reservation);

        return PaymentResponse.from(payment);
    }


    // 환불 — 토스페이먼츠 서버에 취소 요청 후 REFUNDED 처리
    @Override
    @Transactional
    public void refund(Long userId, Long paymentId, RefundRequest request) {
        // 1. orderId로 Payment 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // 2. 본인 결제인지 검증
        if(!payment.getReservation().getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 결제만 환불할 수 있습니다.");
        }

        // 3. PAID 상태인지 확인 (환불 가능 여부) — PAID 상태가 아니면 환불 불가
        if(payment.getStatus() != Payment.PayStatus.PAID) {
            throw new IllegalStateException("결제 완료 상태인 건만 환불할 수 있습니다.");
        }
        // 4. 토스페이먼츠 취소 요청
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((secretKey.trim() + ":").getBytes(StandardCharsets.UTF_8));

            String body = String.format("{\"cancelReason\":\"%s\"}", request.getCancelReason());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tosspayments.com/v1/payments/" + payment.getPaymentKey() + "/cancel"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("토스페이먼츠 환불 실패 [{}]: {}", response.statusCode(), response.body());
                throw new IllegalStateException("토스페이먼츠 환불 실패: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("토스페이먼츠 환불 통신 중 오류 발생", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("토스페이먼츠 API 호출 중 오류가 발생했습니다.", e);
        }
        // 5. 상태 업데이트
        payment.setStatus(Payment.PayStatus.REFUNDED);
    }


    // 결제 실패/취소 시 orderId로 PENDING 결제 + 연관 예약 즉시 취소
    @Override
    @Transactional
    public void cancelPendingByOrderId(String orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() != Payment.PayStatus.PENDING) return;

            Reservation reservation = payment.getReservation();
            if (reservation != null && reservation.getStatus() == Reservation.Status.PENDING) {
                reservation.setStatus(Reservation.Status.CANCELLED);
                reservationRepository.save(reservation);
            }
            paymentRepository.delete(payment);
            log.info("결제 실패로 인한 즉시 취소 처리 — orderId: {}", orderId);
        });
    }


    // 내 결제 내역 조회
    @Override
    public List<PaymentResponse> getMyPayments(Long userId) {
        // 1. userId로 본인 예약 ID 목록 조회
        List<Long> reservationIds = reservationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(Reservation::getId)
                .toList();

        // 2. 예약 ID 목록으로 결제 내역 조회
        List<Payment> payments = paymentRepository.findByReservationIdInOrderByCreatedAtAsc(reservationIds);

        // 3. PaymentResponse로 변환 후 반환
        return payments.stream()
                .map(PaymentResponse::from)
                .toList();
    }
}
