package beauty.beauty.review.dto;

import beauty.beauty.stylist.entity.StylistProfile;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Getter
@Builder
public class SalonReviewsResponse {

    private Long stylistId;
    private String stylistName;
    private String stylistProfileImg;
    private BigDecimal averageRating;
    private int reviewCount;
    private List<ReviewResponse> reviews;

    public static SalonReviewsResponse from(StylistProfile stylist, List<ReviewResponse> reviews) {
        BigDecimal avg = reviews.isEmpty()
                ? BigDecimal.ZERO
                : reviews.stream()
                        .map(ReviewResponse::getRating)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);

        return SalonReviewsResponse.builder()
                .stylistId(stylist.getId())
                .stylistName(stylist.getUser().getName())
                .stylistProfileImg(stylist.getUser().getProfileImg())
                .averageRating(avg)
                .reviewCount(reviews.size())
                .reviews(reviews)
                .build();
    }
}
