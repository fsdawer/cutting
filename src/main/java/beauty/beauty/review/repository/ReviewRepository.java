package beauty.beauty.review.repository;

import beauty.beauty.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r " +
           "JOIN FETCH r.user " +
           "JOIN FETCH r.stylistProfile sp " +
           "JOIN FETCH sp.user " +
           "LEFT JOIN FETCH sp.salon " +
           "JOIN FETCH r.reservation res " +
           "JOIN FETCH res.service " +
           "WHERE sp.id = :stylistId")
    List<Review> findByStylistProfileId(@Param("stylistId") Long stylistId);

    Optional<Review> findByReservationId(Long reservationId);

    // recalculateScore 단건용
    @Query("SELECT COUNT(r) FROM Review r WHERE r.stylistProfile.id = :stylistId")
    int countByStylistProfileId(@Param("stylistId") Long stylistId);

    // getRanking 배치용: N번 쿼리 → 1번 GROUP BY
    @Query("SELECT r.stylistProfile.id, COUNT(r) FROM Review r WHERE r.stylistProfile.id IN :stylistIds GROUP BY r.stylistProfile.id")
    List<Object[]> countGroupByStylistIds(@Param("stylistIds") List<Long> stylistIds);

    @Query("SELECT r FROM Review r " +
           "JOIN FETCH r.user " +
           "JOIN FETCH r.stylistProfile sp " +
           "JOIN FETCH sp.user " +
           "LEFT JOIN FETCH sp.salon " +
           "JOIN FETCH r.reservation res " +
           "JOIN FETCH res.service " +
           "WHERE sp.id IN :stylistIds")
    List<Review> findByStylistProfileIdIn(@Param("stylistIds") List<Long> stylistIds);
}
