package beauty.beauty.reservation.controller;

import beauty.beauty.global.annotation.LoginUserId;
import beauty.beauty.reservation.dto.CursorResponse;
import beauty.beauty.reservation.dto.ReservationRequest;
import beauty.beauty.reservation.dto.ReservationResponse;
import beauty.beauty.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // POST /api/reservations                  예약 생성
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@LoginUserId Long userId,
                                                  @RequestBody ReservationRequest reservationRequest) {

        ReservationResponse response = reservationService.createReservation(userId,reservationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET  /api/reservations/my?lastId=&size=20    내 예약 목록 (No-Offset Cursor)
    // lastId 없으면 첫 페이지, 이후엔 이전 응답의 nextCursorId 값 전달
    @GetMapping("/my")
    public ResponseEntity<CursorResponse<ReservationResponse>> getMyReservations(
            @LoginUserId Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reservationService.getMyReservationsCursor(userId, lastId, size));
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
    // 4. 내 미용실 예약 내역 조회 (No-Offset Cursor)
    @GetMapping("/stylist")
    public ResponseEntity<CursorResponse<ReservationResponse>> getStylistReservations(
            @LoginUserId Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reservationService.getStylistReservations(userId, lastId, size));
    }


    // 5. 미용사가 예약 상태 변경하기 (승인/완료/거절)
    @PutMapping("/{reservationId}/status")
    public ResponseEntity<Void> updateReservationStatus(@LoginUserId Long userId,
                                                        @PathVariable Long reservationId,
                                                        @RequestParam String status) {
        reservationService.updateReservationStatus(userId, reservationId, status);
        return ResponseEntity.ok().build();
    }

    // 이미지 업로드 (선택)
    @PostMapping("/{reservationId}/images")
    public ResponseEntity<Void> uploadImages(
            @LoginUserId Long userId,
            @PathVariable Long reservationId,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) {
        if (images != null && !images.isEmpty()) {
            reservationService.uploadImages(userId, reservationId, images);
        }
        return ResponseEntity.ok().build();
    }

    // 6. 특정 미용사의 특정 날짜 예약된 시간대 조회 (고객용/예약화면용)
    @GetMapping("/stylists/{stylistId}/booked-times")
    public ResponseEntity<List<String>> getStylistBookedTimes(@PathVariable Long stylistId,
                                                              @RequestParam String date) {
        List<String> bookedTimes = reservationService.getStylistBookedTimes(stylistId, date);
        return ResponseEntity.ok(bookedTimes);
    }
}
