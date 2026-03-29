package beauty.beauty.stylist.repository;

import beauty.beauty.stylist.entity.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OperatingHoursRepository extends JpaRepository<OperatingHours, Long> {
    List<OperatingHours> findByStylistProfileId(Long stylistId);

    // 특정 미용사의 특정 요일 운영시간 조회
    Optional<OperatingHours> findByStylistProfileIdAndDayOfWeek(Long stylistId, int dayOfWeek);
}
