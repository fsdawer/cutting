package beauty.beauty.stylist.service;

import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.reservation.repository.ReservationRepository;
import beauty.beauty.stylist.entity.StylistDailyStat;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.repository.StylistDailyStatRepository;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Phase 3] 미용사 CRM 대시보드를 위한 일일 정산(Settlement) 배치
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailySettlementScheduler {

    private final ReservationRepository reservationRepository;
    private final StylistDailyStatRepository statRepository;
    private final StylistProfileRepository stylistProfileRepository;

    // 매일 새벽 2시에 어제 하루 동안의 데이터를 정산
    // 테스트 시 결과를 빨리 보기 위해 1분마다 동작하도록 임시 세팅: cron = "0 * * * * ?"
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void calculateDailyStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);

        log.info("[Settlement Batch] {} 일자 매출 정산 배치를 시작합니다.", yesterday);

        // [Flow 1] DB에서 예약 데이터를 그룹핑하여 집계된 결과만 가져옴
        // (수만 건의 데이터를 메모리에 올리지 않고 DB 단에서 연산 최적화)
        List<Object[]> stats = reservationRepository.getDailyStatsGroupByStylistAndStatus(startOfDay, endOfDay);

        // Map<StylistId, StatDto>
        Map<Long, StatDto> statMap = new HashMap<>();

        for (Object[] row : stats) {
            Long stylistId = (Long) row[0];
            Reservation.Status status = (Reservation.Status) row[1];
            long count = ((Number) row[2]).longValue();
            long revenue = ((Number) row[3]).longValue();

            StatDto dto = statMap.computeIfAbsent(stylistId, k -> new StatDto());

            // 확정되거나 완료된 예약은 매출/건수에 포함
            if (status == Reservation.Status.DONE || status == Reservation.Status.CONFIRMED) {
                dto.reservationCount += count;
                dto.totalRevenue += revenue;
            } 
            // 취소된 예약은 취소 건수에 포함
            else if (status == Reservation.Status.CANCELLED) {
                dto.cancelCount += count;
            }
        }

        // [Flow 2] 집계된 결과를 통계 전용 테이블(StylistDailyStat)에 일괄 저장
        int saveCount = 0;
        for (Map.Entry<Long, StatDto> entry : statMap.entrySet()) {
            Long stylistId = entry.getKey();
            StatDto dto = entry.getValue();

            StylistProfile profile = stylistProfileRepository.findById(stylistId).orElse(null);
            if (profile == null) continue;

            // 이미 정산된 내역이 있다면 덮어쓰기 (멱등성 보장)
            StylistDailyStat stat = statRepository.findByStylistProfileIdAndStatDate(stylistId, yesterday)
                    .orElse(StylistDailyStat.builder()
                            .stylistProfile(profile)
                            .statDate(yesterday)
                            .build());

            // JPA 영속성 컨텍스트를 활용한 업데이트 (또는 새로 생성)
            // 주의: @Builder로 생성된 객체는 필드를 수정할 수 없으므로(Setter 없음) 새로운 객체로 교체하거나 
            // 실무에서는 필드를 업데이트하는 메서드(updateStats)를 만들어 씁니다.
            // 여기서는 심플하게 새로운 엔티티를 통째로 덮어씁니다. (JPA ID가 있으면 update)
            StylistDailyStat newStat = StylistDailyStat.builder()
                    .id(stat.getId())
                    .stylistProfile(profile)
                    .statDate(yesterday)
                    .reservationCount((int) dto.reservationCount)
                    .totalRevenue(dto.totalRevenue)
                    .cancelCount((int) dto.cancelCount)
                    .build();

            statRepository.save(newStat);
            saveCount++;
        }

        log.info("[Settlement Batch] 정산 완료. 총 {} 명의 미용사 통계 생성됨.", saveCount);
    }

    // 통계 집계용 내부 DTO
    private static class StatDto {
        long reservationCount = 0;
        long totalRevenue = 0;
        long cancelCount = 0;
    }
}
