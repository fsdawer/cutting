package beauty.beauty.stylist.repository;

import beauty.beauty.stylist.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.locationtech.jts.geom.Point;
import java.util.List;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    // ST_Distance_Sphere 연산: 반경(radius) 이내의 데이터를 거리 오름차순(가까운 순)으로 정렬하여 반환
    @Query(value = "SELECT s.* FROM salons s WHERE ST_Distance_Sphere(s.location, :userPoint) <= :radius ORDER BY ST_Distance_Sphere(s.location, :userPoint) ASC", nativeQuery = true)
    List<Salon> findSalonsWithinRadius(@Param("userPoint") Point userPoint, @Param("radius") double radius);
}
