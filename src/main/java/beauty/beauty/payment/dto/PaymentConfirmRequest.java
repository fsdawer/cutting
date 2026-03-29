package beauty.beauty.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    // 토스 위젯 결제 완료 후 프론트가 받는 값들 —
    // 그대로 서버로 전달하면 서버에서 토스 API에 검증 요청을 보냅니다.

    // 결제 건 식별 키 (토스 발급)
    private String paymentKey;

    // prepare 단계에서 서버가 발급한 orderId
    private String orderId;

    // 실제 결제된 금액 (서버에서 DB 금액과 일치 여부 검증 필요)
    private int amount;
}
