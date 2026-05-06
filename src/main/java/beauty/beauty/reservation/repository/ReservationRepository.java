package beauty.beauty.reservation.repository;

import beauty.beauty.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Reservation> findByStylistProfileId(Long stylistId);
    List<Reservation> findByUserIdAndStatus(Long userId, Reservation.Status status);
    boolean existsByStylistProfileIdAndReservedAtAndStatusIn(Long stylistId, LocalDateTime reservedAt, List<Reservation.Status> statuses);
    List<Reservation> findByStylistProfileIdAndReservedAtBetweenAndStatusIn(Long stylistId, LocalDateTime start, LocalDateTime end, List<Reservation.Status> statuses);

    // recalculateScore 단건용
    @Query("SELECT COUNT(r) FROM Reservation r " +
           "WHERE r.stylistProfile.id = :stylistId " +
           "AND r.reservedAt >= :since " +
           "AND r.status IN :statuses")
    long countByStylistProfileIdAndReservedAtAfterAndStatusIn(
            @Param("stylistId") Long stylistId,
            @Param("since") LocalDateTime since,
            @Param("statuses") List<Reservation.Status> statuses);

    // getRanking 배치용: N번 쿼리 → 1번 GROUP BY
    @Query("SELECT r.stylistProfile.id, COUNT(r) FROM Reservation r " +
           "WHERE r.stylistProfile.id IN :stylistIds " +
           "AND r.reservedAt >= :since " +
           "AND r.status IN :statuses " +
           "GROUP BY r.stylistProfile.id")
    List<Object[]> countGroupByStylistIdsAndReservedAtAfterAndStatusIn(
            @Param("stylistIds") List<Long> stylistIds,
            @Param("since") LocalDateTime since,
            @Param("statuses") List<Reservation.Status> statuses);

    // [CRM 대시보드] 배치 집계용: 특정 날짜의 미용사별 통계 (상태별 그룹화)
    @Query("SELECT r.stylistProfile.id, r.status, COUNT(r), COALESCE(SUM(r.totalPrice), 0) " +
           "FROM Reservation r " +
           "WHERE r.reservedAt >= :start AND r.reservedAt < :end " +
           "GROUP BY r.stylistProfile.id, r.status")
    List<Object[]> getDailyStatsGroupByStylistAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
