package beauty.beauty.reservation.entity;

import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.entity.StylistServiceItem;
import beauty.beauty.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
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
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservationImage> images = new ArrayList<>();

    public enum Status { PENDING, CONFIRMED, DONE, CANCELLED }
}
