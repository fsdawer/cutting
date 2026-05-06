package beauty.beauty.notification.service;

import beauty.beauty.notification.dto.NotificationMessage;
import beauty.beauty.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Redis Stream 리스너에서 호출되며 @Async로 별도 스레드에서 WebSocket 전송.
 * Stream 소비 스레드를 블로킹하지 않음.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Async("reservationTaskExecutor")
    public void notifyReservationCreated(Reservation reservation) {
        // [Flow 1] 알림에 필요한 메타데이터 추출
        Long stylistUserId = reservation.getStylistProfile().getUser().getId();
        Long clientUserId = reservation.getUser().getId();

        // [Flow 2] 알림 메시지 객체 생성
        NotificationMessage msg = NotificationMessage.builder()
                .type("RESERVATION_CREATED")
                .reservationId(reservation.getId())
                .stylistName(reservation.getStylistProfile().getUser().getName())
                .clientName(reservation.getUser().getName())
                .reservedAt(reservation.getReservedAt().toString())
                .message("새 예약이 확정되었습니다.")
                .build();

        // [Flow 3] WebSocket / Message Broker를 통한 실제 전송 (외부 네트워크 I/O)
        // 이 전송 작업이 느려져도 메인 스레드나 Redis Stream Consumer를 막지 않도록 @Async 스레드풀을 사용합니다.
        // 동기 전송 1: 미용사에게 알림
        messagingTemplate.convertAndSend("/topic/notification/" + stylistUserId, msg);
        // 동기 전송 2: 고객에게 알림
        messagingTemplate.convertAndSend("/topic/notification/" + clientUserId, msg);
    }

    @Async("reservationTaskExecutor")
    public void notifyWaitingAvailable(Long userId, LocalDate date, LocalTime time) {
        String dateLabel = date.getMonthValue() + "월 " + date.getDayOfMonth() + "일 " + time;
        NotificationMessage msg = NotificationMessage.builder()
                .type("WAITING_AVAILABLE")
                .reservedAt(date + "T" + time)
                .message(dateLabel + " 빈자리가 생겼습니다!")
                .build();
        messagingTemplate.convertAndSend("/topic/notification/" + userId, msg);
    }
}
