package beauty.beauty.reservation.dto;

import beauty.beauty.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long stylistId;
    private String stylistName;
    private String salonName;
    private String stylistProfileImg;
    private String serviceName;
    private LocalDateTime reservedAt;
    private String status;
    private int totalPrice;
    private Long chatRoomId; // 확정되어 채팅방이 생성된 경우


    public static ReservationResponse from(Reservation reservation, Long chatRoomId) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .stylistId(reservation.getStylistProfile().getId())
                .stylistName(reservation.getStylistProfile().getUser().getName())
                .salonName(reservation.getStylistProfile().getSalonName())
                .stylistProfileImg(reservation.getStylistProfile().getUser().getProfileImg())
                .serviceName(reservation.getService().getName())
                .reservedAt(reservation.getReservedAt())
                .status(reservation.getStatus().name())
                .totalPrice(reservation.getTotalPrice())
                .chatRoomId(chatRoomId)
                .build();
    }
}
