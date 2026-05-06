package beauty.beauty.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefundRequest {

    // 환불 사유 (토스 API 필수값)
    private String cancelReason;
}
