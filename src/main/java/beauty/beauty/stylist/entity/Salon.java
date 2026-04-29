package beauty.beauty.stylist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "salons")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Salon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
