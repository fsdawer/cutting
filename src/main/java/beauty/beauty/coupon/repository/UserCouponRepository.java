package beauty.beauty.coupon.repository;

import beauty.beauty.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    // 내 쿠폰 목록 (미사용 + 유효기간 내)
    @Query("""
            SELECT uc FROM UserCoupon uc
            JOIN FETCH uc.coupon c
            WHERE uc.user.id = :userId
              AND uc.isUsed = false
              AND c.startAt <= CURRENT_TIMESTAMP
              AND c.expiredAt > CURRENT_TIMESTAMP
            ORDER BY c.expiredAt ASC
            """)
    List<UserCoupon> findValidCouponsByUserId(@Param("userId") Long userId);

    // 쿠폰 사용 시 유효성 검증용 단건 조회
    @Query("""
            SELECT uc FROM UserCoupon uc
            JOIN FETCH uc.coupon c
            WHERE uc.id = :userCouponId
              AND uc.user.id = :userId
              AND uc.isUsed = false
              AND c.startAt <= CURRENT_TIMESTAMP
              AND c.expiredAt > CURRENT_TIMESTAMP
            """)
    Optional<UserCoupon> findValidOne(@Param("userCouponId") Long userCouponId,
                                      @Param("userId") Long userId);


    // 중복 발급 체크
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
