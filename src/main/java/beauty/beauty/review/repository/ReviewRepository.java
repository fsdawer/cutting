package beauty.beauty.review.repository;

import beauty.beauty.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByStylistProfileId(Long stylistId);
    Optional<Review> findByReservationId(Long reservationId);
}
