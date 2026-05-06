package beauty.beauty.notification.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String type;           // RESERVATION_CREATED, RESERVATION_CANCELLED 등
    private Long reservationId;
    private String stylistName;
    private String clientName;
    private String reservedAt;
    private String message;
}
