package beauty.beauty.review.service;

import beauty.beauty.reservation.dto.ReservationResponse;
import beauty.beauty.review.dto.ReviewRequest;
import beauty.beauty.review.dto.ReviewResponse;
import beauty.beauty.review.dto.ReviewUpdateRequest;
import beauty.beauty.review.dto.SalonReviewsResponse;

import java.util.List;

public interface ReviewService {

    // 리뷰 작성 — status=DONE 인 예약만 가능, 예약 1건당 1개
    ReviewResponse create(Long userId, ReviewRequest request);

    // 미용사별 리뷰 목록
    // GET /api/reviews/stylist/{stylistId}
    List<ReviewResponse> getByStylist(Long stylistId);

    // 미용실 내 미용사별 리뷰 묶음 조회
    // GET /api/reviews/salon/{salonId}
    // → [{ stylistId, stylistName, averageRating, reviews: [...] }, ...]
    List<SalonReviewsResponse> getBySalon(Long salonId);

    // 리뷰 수정 — 본인만 가능
    ReviewResponse update(Long userId, Long reviewId, ReviewUpdateRequest request);

    // 리뷰 삭제 — 본인만 가능, stylistProfile.rating/reviewCount 재계산
    void delete(Long userId, Long reviewId);
}
