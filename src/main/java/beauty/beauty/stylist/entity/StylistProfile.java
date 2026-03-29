package beauty.beauty.stylist.entity;

import beauty.beauty.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stylist_profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StylistProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "salon_name", length = 100)
    private String salonName;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false)
    private int experience = 0;

    @Column(nullable = false)
    private double rating = 0.0;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;
}
