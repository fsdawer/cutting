package beauty.beauty.review.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    // POST /api/reviews                리뷰 작성 (status=DONE 인 예약만 가능)
    // GET  /api/reviews/stylist/{id}   미용사별 리뷰 목록
    // PUT  /api/reviews/{id}           리뷰 수정
    // DELETE /api/reviews/{id}         리뷰 삭제
}
