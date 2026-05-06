package beauty.beauty.ranking.service;

import beauty.beauty.ranking.dto.RankingResponse;

import java.util.List;

public interface RankingService {
    List<RankingResponse> getRanking(String district);
    // StylistProfile 엔티티가 아닌 ID를 받아야 AOP 프록시가 정상 적용됨 → @Transactional 작동
    void recalculateScore(Long stylistProfileId);
}
