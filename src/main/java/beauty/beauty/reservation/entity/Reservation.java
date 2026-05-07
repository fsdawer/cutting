package beauty.beauty.reservation.entity;

import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.entity.StylistServiceItem;
import beauty.beauty.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations", indexes = {
    // 내 예약 목록 페이징: WHERE user_id = ? ORDER BY created_at DESC
    @Index(name = "idx_res_user_created", columnList = "user_id, created_at"),
    // 스타일리스트 슬롯 조회 / 분산락 중복 체크: WHERE stylist_id = ? AND reserved_at = ?
    @Index(name = "idx_res_stylist_reserved", columnList = "stylist_id, reserved_at"),
    // 랭킹 집계 / 최근 예약수 카운트: WHERE stylist_id = ? AND reserved_at >= ? AND status IN (?)
    @Index(name = "idx_res_stylist_status_reserved", columnList = "stylist_id, status, reserved_at"),
    // 리마인더 스케줄러: WHERE reserved_at BETWEEN ? AND ? AND status = ?
    @Index(name = "idx_res_reserved_status", columnList = "reserved_at, status"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_id", nullable = false)
    private StylistProfile stylistProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private StylistServiceItem service;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "request_memo", columnDefinition = "TEXT")
    private String requestMemo;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @BatchSize(size = 20)
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservationImage> images = new ArrayList<>();

    public enum Status { PENDING, CONFIRMED, DONE, CANCELLED }
}
