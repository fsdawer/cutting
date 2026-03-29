package beauty.beauty.stylist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "operating_hours",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stylist_id", "day_of_week"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatingHours {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_id", nullable = false)
    private StylistProfile stylistProfile;

    /** 0=월, 1=화, ..., 6=일 */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private boolean isClosed = false;


    // 영업시간 업데이트 로직 추가
    public void update(LocalTime openTime, LocalTime closeTime, boolean isClosed) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isClosed = isClosed;
    }
}
