package beauty.beauty.coupon.repository;

import beauty.beauty.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    // 미용사가 발급한 쿠폰 목록
    List<Coupon> findByStylistIdOrderByCreatedAtDesc(Long stylistId);
}
