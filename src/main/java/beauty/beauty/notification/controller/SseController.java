package beauty.beauty.notification.controller;

import beauty.beauty.global.annotation.LoginUserId;
import beauty.beauty.notification.dto.NotificationMessage;
import beauty.beauty.notification.service.NotificationService;
import beauty.beauty.notification.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService    sseEmitterService;
    private final NotificationService  notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@LoginUserId Long userId) {
        SseEmitter emitter = sseEmitterService.connect(userId);

        // 연결 직후 미전달 알림 flush (Toss 결제 중 끊겼을 때 보관된 알림 전달)
        List<NotificationMessage> pending = notificationService.flushPending(userId);
        for (NotificationMessage msg : pending) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(msg, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.warn("[SSE] pending flush 중 오류 - userId: {}", userId, e);
                break;
            }
        }

        return emitter;
    }
}
