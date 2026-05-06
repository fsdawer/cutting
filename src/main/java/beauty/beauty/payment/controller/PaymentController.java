package beauty.beauty.payment.controller;

import beauty.beauty.global.annotation.LoginUserId;
import beauty.beauty.payment.dto.*;
import beauty.beauty.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/payments/prepare       결제 준비 (orderId 생성 + Payment PENDING 저장)
    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> prepare(@LoginUserId Long userId,
                                                          @RequestBody PaymentPrepareRequest request) {
        return ResponseEntity.ok(paymentService.prepare(userId, request));
    }

    // POST /api/payments/confirm       결제 승인 (토스페이먼츠 서버 검증 후 PAID 처리)
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirm(@LoginUserId Long userId,
                                                   @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirm(userId, request));
    }

    // POST /api/payments/{id}/refund   환불 요청 (REFUNDED 처리)
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Void> refund(@LoginUserId Long userId,
                                       @PathVariable Long paymentId,
                                       @RequestBody RefundRequest request) {
        paymentService.refund(userId, paymentId, request);
        return ResponseEntity.ok().build();
    }

    // GET  /api/payments/my            내 결제 내역 조회
    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(@LoginUserId Long userId) {
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }

    // POST /api/payments/cancel-pending  결제 실패/취소 시 orderId로 PENDING 즉시 취소 (인증 불필요)
    @PostMapping("/cancel-pending")
    public ResponseEntity<Void> cancelPending(@RequestParam String orderId) {
        paymentService.cancelPendingByOrderId(orderId);
        return ResponseEntity.ok().build();
    }

    // GET /api/payments/by-order?orderId=  orderId로 단건 조회 (결제 성공 화면용)
    @GetMapping("/by-order")
    public ResponseEntity<PaymentResponse> getByOrderId(@LoginUserId Long userId,
                                                        @RequestParam String orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(userId, orderId));
    }
}
