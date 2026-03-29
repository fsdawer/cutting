package beauty.beauty.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPrepareResponse {

    // DB에 저장된 Payment PK (환불 등 후속 요청 시 사용)
    private Long paymentId;

    // 토스 위젯 requestPayment() 에 넘길 orderId
    private String orderId;

    // 토스 위젯 setAmount() 에 넘길 결제 금액
    private int amount;
}
