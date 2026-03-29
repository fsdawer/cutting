package beauty.beauty.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reservation_images")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
}
