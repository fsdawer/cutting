package beauty.beauty.stylist.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stylist_services")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StylistServiceItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_id", nullable = false)
    private StylistProfile stylistProfile;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int duration;

    @Column(length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;



    // 서비스 수정 로직 캡슐화
    public void update(String name, int price, int duration, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (price > 0) {
            this.price = price;
        }
        if (duration > 0) {
            this.duration = duration;
        }
        if (description != null) {
            this.description = description;
        }
    }
}
