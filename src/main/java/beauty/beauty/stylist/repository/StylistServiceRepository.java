package beauty.beauty.stylist.repository;

import beauty.beauty.stylist.entity.StylistServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StylistServiceRepository extends JpaRepository<StylistServiceItem, Long> {
    List<StylistServiceItem> findByStylistProfileIdAndIsActiveTrue(Long stylistId);
}
