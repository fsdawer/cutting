package beauty.beauty.ranking.controller;

import beauty.beauty.ranking.dto.RankingResponse;
import beauty.beauty.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // GET /api/ranking?district=강남구
    @GetMapping
    public ResponseEntity<List<RankingResponse>> getRanking(
            @RequestParam String district) {
        return ResponseEntity.ok(rankingService.getRanking(district));
    }
}
