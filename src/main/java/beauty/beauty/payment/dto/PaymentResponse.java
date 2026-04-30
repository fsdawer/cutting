package beauty.beauty.payment.dto;

import beauty.beauty.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long reservationId;
    private String orderId;
    private String paymentKey;
    private int amount;
    private String method;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // 결제 카드 표시용
    private String stylistName;
    private String salonName;
    private String serviceName;

    public static PaymentResponse from(Payment payment) {
        var sp = payment.getReservation().getStylistProfile();
        return PaymentResponse.builder()
                .id(payment.getId())
                .reservationId(payment.getReservation().getId())
                .orderId(payment.getOrderId())
                .paymentKey(payment.getPaymentKey())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .status(payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .stylistName(sp.getUser().getName())
                .salonName(sp.getSalon() != null ? sp.getSalon().getName() : null)
                .serviceName(payment.getReservation().getService().getName())
                .build();
    }
}
