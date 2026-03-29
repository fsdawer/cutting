package beauty.beauty.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentPrepareRequest {

    // 결제할 예약 ID
    private Long reservationId;
}
