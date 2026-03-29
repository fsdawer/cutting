package beauty.beauty.payment.service;

import beauty.beauty.payment.dto.*;

import java.util.List;

public interface PaymentService {

    // 결제 준비 — orderId 발급 + Payment(PENDING) DB 저장
    // userId: 로그인한 사용자 ID
    PaymentPrepareResponse prepare(Long userId, PaymentPrepareRequest request);

    // 결제 승인 — 토스페이먼츠 서버에 최종 승인 요청 후 PAID로 업데이트
    // userId: 본인 결제인지 검증용
    PaymentResponse confirm(Long userId, PaymentConfirmRequest request);

    // 환불 — 토스페이먼츠 서버에 취소 요청 후 REFUNDED로 업데이트
    // paymentId: DB Payment PK
    void refund(Long userId, Long paymentId, RefundRequest request);

    // 내 결제 내역 조회
    List<PaymentResponse> getMyPayments(Long userId);
}
