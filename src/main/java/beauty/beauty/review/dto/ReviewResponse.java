package beauty.beauty.review.dto;

import beauty.beauty.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long reservationId;

    // 작성자 정보
    private Long userId;
    private String userName;
    private String userProfileImg;

    // 미용사 & 미용실 정보
    private Long stylistId;
    private String stylistName;
    private String salonName;

    // 예약한 서비스명
    private String serviceName;

    // 리뷰 내용
    private BigDecimal rating;
    private String content;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .reservationId(review.getReservation().getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .userProfileImg(review.getUser().getProfileImg())
                .stylistId(review.getStylistProfile().getId())
                .stylistName(review.getStylistProfile().getUser().getName())
                .salonName(review.getStylistProfile().getSalon() != null
                        ? review.getStylistProfile().getSalon().getName() : null)
                .serviceName(review.getReservation().getService().getName())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
