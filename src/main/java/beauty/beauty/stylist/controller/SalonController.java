package beauty.beauty.stylist.controller;

import beauty.beauty.stylist.entity.Salon;
import beauty.beauty.stylist.repository.SalonRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salons")
@RequiredArgsConstructor
public class SalonController {

    private final SalonRepository salonRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    // 내 주변 미용실 찾기 (GET /api/salons/nearby?lat=37.498&lng=127.027&radius=3000)
    @GetMapping("/nearby")
    public ResponseEntity<List<Salon>> getNearbySalons(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") double radius) {

        // 위도, 경도를 바탕으로 Point 객체 생성 (주의: Point는 x=경도(lng), y=위도(lat) 순서)
        Point userLocation = geometryFactory.createPoint(new Coordinate(lng, lat));

        // 공간 인덱스를 활용해 반경 내 미용실 조회
        List<Salon> salons = salonRepository.findSalonsWithinRadius(userLocation, radius);

        return ResponseEntity.ok(salons);
    }
}
