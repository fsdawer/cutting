package beauty.beauty.reservation.controller;

import beauty.beauty.global.annotation.LoginUserId;
import beauty.beauty.reservation.service.WaitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/waiting")
@RequiredArgsConstructor
public class WaitingController {

    private final WaitingService waitingService;

    @PostMapping("/stylists/{stylistProfileId}")
    public ResponseEntity<String> registerWaiting(
            @LoginUserId Long userId,
            @PathVariable Long stylistProfileId,
            @RequestParam String date,
            @RequestParam String time) {
        
        waitingService.registerWaiting(userId, stylistProfileId, LocalDate.parse(date), LocalTime.parse(time));
        return ResponseEntity.ok("빈자리 알림 신청이 완료되었습니다.");
    }
}
