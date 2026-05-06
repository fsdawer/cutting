package beauty.beauty.favorite.repository;

import beauty.beauty.favorite.entity.FavoriteStylist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FavoriteStylistRepository extends JpaRepository<FavoriteStylist, Long> {
    Optional<FavoriteStylist> findByUserIdAndStylistProfileId(Long userId, Long stylistProfileId);
    boolean existsByUserIdAndStylistProfileId(Long userId, Long stylistProfileId);
}
