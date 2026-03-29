package beauty.beauty.reservation.controller;

import beauty.beauty.global.annotation.LoginUserId;
import beauty.beauty.reservation.dto.ReservationRequest;
import beauty.beauty.reservation.dto.ReservationResponse;
import beauty.beauty.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // POST /api/reservations                  예약 생성
    @PostMapping
    public ResponseEntity<Void> createReservation(@LoginUserId Long userId,
                                                  @RequestBody ReservationRequest reservationRequest) {

        reservationService.createReservation(userId,reservationRequest);
        return ResponseEntity.ok().build();
    }

    // GET  /api/reservations/my               내 예약 목록
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(@LoginUserId Long userId) {

        List<ReservationResponse> getReservation = reservationService.getMyReservations(userId);
        return ResponseEntity.ok(getReservation);

    }

    // GET  /api/reservations/{id}             예약 상세
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservationDetail(@LoginUserId Long userId,
                                                                    @PathVariable Long reservationId) {
        ReservationResponse reservationResponse = reservationService.getReservationById(userId, reservationId);
        return ResponseEntity.ok(reservationResponse);
    }

    // 3. 예약 취소하기
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> cancelReservation(@LoginUserId Long userId,
                                                  @PathVariable Long reservationId) {
        reservationService.cancelReservation(userId, reservationId);
        return ResponseEntity.ok().build();
    }



    // 미용사 전용 ========
    // 4. 내 미용실 예약 내역 조회
    @GetMapping("/stylist")
    public ResponseEntity<List<ReservationResponse>> getStylistReservations(@LoginUserId Long userId) {
        return ResponseEntity.ok(reservationService.getStylistReservations(userId));
    }


    // 5. 미용사가 예약 상태 변경하기 (승인/완료/거절)
    @PutMapping("/{reservationId}/status")
    public ResponseEntity<Void> updateReservationStatus(@LoginUserId Long userId,
                                                        @PathVariable Long reservationId,
                                                        @RequestParam String status) {
        reservationService.updateReservationStatus(userId, reservationId, status);
        return ResponseEntity.ok().build();
    }
}
