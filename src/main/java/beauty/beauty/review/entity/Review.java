package beauty.beauty.review.entity;

import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", indexes = {
    // 미용사 리뷰 목록: WHERE stylist_id = ? ORDER BY created_at DESC
    @Index(name = "idx_review_stylist_created", columnList = "stylist_id, created_at"),
    // 내 리뷰 목록: WHERE user_id = ?
    @Index(name = "idx_review_user", columnList = "user_id"),
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_id", nullable = false)
    private StylistProfile stylistProfile;

    @Column(nullable = false)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
