package beauty.beauty.notification.controller;

import beauty.beauty.global.annotation.LoginUserId;
import beauty.beauty.notification.dto.NotificationMessage;
import beauty.beauty.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/pending")
    public ResponseEntity<List<NotificationMessage>> getPending(@LoginUserId Long userId) {
        return ResponseEntity.ok(notificationService.flushPending(userId));
    }
}
