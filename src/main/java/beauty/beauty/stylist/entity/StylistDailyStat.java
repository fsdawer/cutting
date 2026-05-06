package beauty.beauty.stylist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "stylist_daily_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"stylist_profile_id", "stat_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StylistDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_profile_id", nullable = false)
    private StylistProfile stylistProfile;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    // 해당 일자의 총 예약 완료 건수 (DONE 상태 기준)
    @Column(name = "reservation_count", nullable = false)
    @Builder.Default
    private int reservationCount = 0;

    // 해당 일자의 총매출 (DONE 상태 기준)
    @Column(name = "total_revenue", nullable = false)
    @Builder.Default
    private long totalRevenue = 0;

    // 해당 일자의 노쇼 및 취소 건수
    @Column(name = "cancel_count", nullable = false)
    @Builder.Default
    private int cancelCount = 0;
}
