package beauty.beauty.stylist.repository;

import beauty.beauty.stylist.entity.StylistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StylistProfileRepository extends JpaRepository<StylistProfile, Long> {

    Optional<StylistProfile> findByUserId(Long userId);

    @Query("SELECT s FROM StylistProfile s WHERE " +
            "(:keyword IS NULL OR s.salonName LIKE %:keyword% OR s.user.name LIKE %:keyword%) AND " +
            "(:location IS NULL OR s.location LIKE %:location%)")
    List<StylistProfile> searchStylists(@Param("keyword") String keyword, @Param("location") String location);
}
