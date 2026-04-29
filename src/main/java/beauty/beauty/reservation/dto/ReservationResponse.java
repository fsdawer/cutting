package beauty.beauty.reservation.dto;

import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.reservation.entity.ReservationImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long stylistId;
    private String stylistName;
    private String salonName;
    private String location;
    private String stylistProfileImg;
    private String serviceName;
    private LocalDateTime reservedAt;
    private String status;
    private int totalPrice;
    private Long chatRoomId;

    // 고객 정보
    private String userName;
    private String userPhone;

    // 요구사항 & 첨부 이미지
    private String requestMemo;
    private List<String> imageUrls;

    public static ReservationResponse from(Reservation reservation, Long chatRoomId) {
        List<String> urls = reservation.getImages() == null
                ? Collections.emptyList()
                : reservation.getImages().stream()
                        .map(ReservationImage::getImageUrl)
                        .toList();

        return ReservationResponse.builder()
                .id(reservation.getId())
                .stylistId(reservation.getStylistProfile().getId())
                .stylistName(reservation.getStylistProfile().getUser().getName())
                .salonName(reservation.getStylistProfile().getSalon() != null ? reservation.getStylistProfile().getSalon().getName() : null)
                .location(reservation.getStylistProfile().getSalon() != null ? reservation.getStylistProfile().getSalon().getAddress() : null)
                .stylistProfileImg(reservation.getStylistProfile().getUser().getProfileImg())
                .serviceName(reservation.getService().getName())
                .reservedAt(reservation.getReservedAt())
                .status(reservation.getStatus().name())
                .totalPrice(reservation.getTotalPrice())
                .chatRoomId(chatRoomId)
                .userName(reservation.getUser().getName())
                .userPhone(reservation.getUser().getPhone())
                .requestMemo(reservation.getRequestMemo())
                .imageUrls(urls)
                .build();
    }
}
