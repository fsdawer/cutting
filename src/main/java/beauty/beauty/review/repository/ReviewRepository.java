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
