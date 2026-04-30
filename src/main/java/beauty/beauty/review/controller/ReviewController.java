package beauty.beauty.review.controller;

import beauty.beauty.global.annotation.LoginUserId;
import beauty.beauty.review.dto.ReviewRequest;
import beauty.beauty.review.dto.ReviewResponse;
import beauty.beauty.review.dto.ReviewUpdateRequest;
import beauty.beauty.review.dto.SalonReviewsResponse;
import beauty.beauty.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // POST /api/reviews
    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @LoginUserId Long userId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(userId, request));
    }

    // GET /api/reviews/stylist/{stylistId}
    @GetMapping("/stylist/{stylistId}")
    public ResponseEntity<List<ReviewResponse>> getByStylist(@PathVariable Long stylistId) {
        return ResponseEntity.ok(reviewService.getByStylist(stylistId));
    }

    // GET /api/reviews/salon/{salonId}
    @GetMapping("/salon/{salonId}")
    public ResponseEntity<List<SalonReviewsResponse>> getBySalon(@PathVariable Long salonId) {
        return ResponseEntity.ok(reviewService.getBySalon(salonId));
    }

    // PUT /api/reviews/{reviewId}
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> update(
            @LoginUserId Long userId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(reviewService.update(userId, reviewId, request));
    }

    // DELETE /api/reviews/{reviewId}
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @LoginUserId Long userId,
            @PathVariable Long reviewId) {
        reviewService.delete(userId, reviewId);
        return ResponseEntity.noContent().build();
    }
}
