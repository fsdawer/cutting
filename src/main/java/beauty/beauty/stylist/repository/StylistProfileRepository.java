package beauty.beauty.stylist.repository;

import beauty.beauty.stylist.entity.StylistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StylistProfileRepository extends JpaRepository<StylistProfile, Long> {

    Optional<StylistProfile> findByUserId(Long userId);

    // N+1 해결용: userId 목록으로 (userId, salonName) 쌍을 한 번에 조회
    @Query("SELECT s.user.id, s.salon.name FROM StylistProfile s LEFT JOIN s.salon WHERE s.user.id IN :userIds")
    List<Object[]> findSalonNamesByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("SELECT DISTINCT s FROM StylistProfile s LEFT JOIN s.salon sal WHERE " +
            "(:keyword IS NULL OR sal.name LIKE %:keyword% OR s.user.name LIKE %:keyword%) AND " +
            "(:location IS NULL OR sal.address LIKE %:location%)")
    List<StylistProfile> searchStylists(@Param("keyword") String keyword, @Param("location") String location);
}
