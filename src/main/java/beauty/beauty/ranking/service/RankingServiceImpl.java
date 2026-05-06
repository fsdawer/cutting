package beauty.beauty.ranking.service;

import beauty.beauty.ranking.dto.RankingResponse;
import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.reservation.repository.ReservationRepository;
import beauty.beauty.review.repository.ReviewRepository;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final StylistProfileRepository stylistProfileRepository;
    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RANKING_KEY_PREFIX = "ranking:";
    private static final int TOP_N = 10;

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> getRanking(String district) {
        String key = RANKING_KEY_PREFIX + district;
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, TOP_N - 1);

        if (tuples == null || tuples.isEmpty()) {
            log.info("[Ranking] 캐시 미스 — district={}, DB에서 워밍업", district);
            return warmUpAndGet(district);
        }

        List<Long> stylistIds = tuples.stream()
                .map(t -> Long.parseLong((String) t.getValue()))
                .toList();

        List<StylistProfile> stylists = stylistProfileRepository.findAllById(stylistIds);
        Map<Long, StylistProfile> stylistMap = stylists.stream()
                .collect(Collectors.toMap(StylistProfile::getId, s -> s));

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Reservation.Status> validStatuses = List.of(Reservation.Status.CONFIRMED, Reservation.Status.DONE);

        Map<Long, Integer> reviewCountMap = reviewRepository.countGroupByStylistIds(stylistIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

        Map<Long, Long> bookingCountMap = reservationRepository
                .countGroupByStylistIdsAndReservedAtAfterAndStatusIn(stylistIds, since, validStatuses).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        List<RankingResponse> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long stylistId = Long.parseLong((String) tuple.getValue());
            StylistProfile sp = stylistMap.get(stylistId);
            if (sp == null) continue;

            int reviewCount = reviewCountMap.getOrDefault(stylistId, 0);
            int recentBookings = bookingCountMap.getOrDefault(stylistId, 0L).intValue();
            double score = tuple.getScore() != null ? tuple.getScore() : 0.0;

            result.add(RankingResponse.from(sp, score, reviewCount, recentBookings));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public void recalculateScore(Long stylistProfileId) {
        StylistProfile stylistProfile = stylistProfileRepository.findById(stylistProfileId)
                .orElseThrow(() -> new IllegalArgumentException("미용사 프로필을 찾을 수 없습니다: " + stylistProfileId));

        int reviewCount = reviewRepository.countByStylistProfileId(stylistProfile.getId());
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Reservation.Status> validStatuses = List.of(Reservation.Status.CONFIRMED, Reservation.Status.DONE);
        long recentBookings = reservationRepository.countByStylistProfileIdAndReservedAtAfterAndStatusIn(
                stylistProfile.getId(), since, validStatuses);

        double score = computeBayesianScore(
                reviewCount, stylistProfile.getRating().doubleValue(), (int) recentBookings);

        String district = stylistProfile.getSalon() != null ? stylistProfile.getSalon().getDistrict() : null;
        if (district != null) {
            String key = RANKING_KEY_PREFIX + district;
            redisTemplate.opsForZSet().add(key, String.valueOf(stylistProfile.getId()), score);
            log.debug("[Ranking] ZADD — stylistId={}, district={}, score={}", stylistProfile.getId(), district, score);
        }
    }

    @Transactional(readOnly = true)
    protected List<RankingResponse> warmUpAndGet(String district) {
        List<StylistProfile> stylists = stylistProfileRepository.findByDistrict(district);
        if (stylists.isEmpty()) return List.of();

        List<Long> stylistIds = stylists.stream().map(StylistProfile::getId).toList();
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Reservation.Status> validStatuses = List.of(Reservation.Status.CONFIRMED, Reservation.Status.DONE);
        String key = RANKING_KEY_PREFIX + district;

        Map<Long, Integer> reviewCountMap = reviewRepository.countGroupByStylistIds(stylistIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));
        Map<Long, Long> bookingCountMap = reservationRepository
                .countGroupByStylistIdsAndReservedAtAfterAndStatusIn(stylistIds, since, validStatuses).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        List<RankingResponse> result = new ArrayList<>();
        for (StylistProfile sp : stylists) {
            int reviewCount = reviewCountMap.getOrDefault(sp.getId(), 0);
            int recentBookings = bookingCountMap.getOrDefault(sp.getId(), 0L).intValue();
            double score = computeBayesianScore(reviewCount, sp.getRating().doubleValue(), recentBookings);

            redisTemplate.opsForZSet().add(key, String.valueOf(sp.getId()), score);
            result.add(RankingResponse.from(sp, score, reviewCount, recentBookings));
        }

        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return result.stream().limit(TOP_N).toList();
    }

    private double computeBayesianScore(int reviewCount, double avgRating, int recentBookings) {
        double bayesian = (avgRating * reviewCount) / (reviewCount + 5.0) * 40;
        double bookingBonus = Math.min(recentBookings, 200) * 0.3;
        return bayesian + bookingBonus;
    }
}
