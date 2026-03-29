package beauty.beauty.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @NotNull(message = "미용사 ID는 필수입니다.")
    private Long stylistId;

    @NotNull(message = "예약할 서비스 ID는 필수입니다.")
    private Long serviceId;

    @NotNull(message = "예약 시간은 필수입니다.")
    @Future(message = "예약 시간은 과거일 수 없습니다.")
    private LocalDateTime reservedAt;

    private String requestMemo;
}
