package beauty.beauty.ranking.dto;

import beauty.beauty.stylist.entity.StylistProfile;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {

    private Long stylistId;
    private String stylistName;
    private String salonName;
    private String district;
    private double avgRating;
    private int reviewCount;
    private int recentBookings;  // 최근 30일 예약 수
    private double score;        // 베이지안 점수

    public static RankingResponse from(StylistProfile sp, double score, int reviewCount, int recentBookings) {
        return RankingResponse.builder()
                .stylistId(sp.getId())
                .stylistName(sp.getUser().getName())
                .salonName(sp.getSalon() != null ? sp.getSalon().getName() : null)
                .district(sp.getSalon() != null ? sp.getSalon().getDistrict() : null)
                .avgRating(sp.getRating().doubleValue())
                .reviewCount(reviewCount)
                .recentBookings(recentBookings)
                .score(score)
                .build();
    }

    // After 1: Redis 캐시 히트 시 — reviewCount/recentBookings는 score에 이미 반영됨
    public static RankingResponse fromCache(StylistProfile sp, double score) {
        return RankingResponse.builder()
                .stylistId(sp.getId())
                .stylistName(sp.getUser().getName())
                .salonName(sp.getSalon() != null ? sp.getSalon().getName() : null)
                .district(sp.getSalon() != null ? sp.getSalon().getDistrict() : null)
                .avgRating(sp.getRating().doubleValue())
                .score(score)
                .build();
    }
}
