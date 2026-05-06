package beauty.beauty.stylist.repository;

import beauty.beauty.stylist.entity.StylistDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StylistDailyStatRepository extends JpaRepository<StylistDailyStat, Long> {
    Optional<StylistDailyStat> findByStylistProfileIdAndStatDate(Long stylistProfileId, LocalDate statDate);
    
    // 특정 미용사의 특정 기간 동안의 통계 조회
    List<StylistDailyStat> findByStylistProfileIdAndStatDateBetweenOrderByStatDateAsc(
            Long stylistProfileId, LocalDate startDate, LocalDate endDate);
}
