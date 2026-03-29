package beauty.beauty.stylist.repository;

import beauty.beauty.stylist.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByStylistProfileId(Long stylistId);
}
