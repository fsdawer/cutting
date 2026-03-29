package beauty.beauty.reservation.service;

import beauty.beauty.reservation.dto.ReservationRequest;
import beauty.beauty.reservation.dto.ReservationResponse;

import java.util.List;

public interface ReservationService {

    /**
     * 1. 예약 생성하기 (일반 사용자 -> 미용사)
     * 사용자가 선택한 미용사(stylistId)와 서비스(serviceId) 정보로 새로운 예약을 등록합니다.
     * 예약 상태는 기본적으로 PENDING(대기) 상태가 됩니다.
     *
     * @param userId 예약을 요청하는 일반 사용자(USER)의 ID
     * @param request 예약 시 필요한 정보 (미용사 ID, 서비스 ID, 예약 시간, 요청 메모)
     */
    void createReservation(Long userId, ReservationRequest request);



    /**
     * 2. 내 예약 내역 가져오기 (마이페이지용)
     * 일반 사용자(USER)가 본인이 예약한 전체 내역을 조회합니다.
     *
     * @param userId 예약을 조회하려는 사용자(USER)의 ID
     * @return 사용자의 예약 내역 목록 (ReservationResponse DTO 리스트)
     */
    List<ReservationResponse> getMyReservations(Long userId);



    /**
     * 특정 예약 상세 조회
     * 일반 사용자나 미용사가 특정 예약의 상세 정보(요청 메모 등)를 확인합니다.
     */
    ReservationResponse getReservationById(Long userId, Long reservationId);




    /**
     * 3. 예약 취소하기
     * 일반 사용자(USER)가 본인의 예약을 취소합니다. (상태가 CANCELLED로 변경됨)
     * 
     * @param userId 예약을 취소하려는 사용자(USER)의 ID
     * @param reservationId 취소하려는 예약 엔티티의 ID
     */
    void cancelReservation(Long userId, Long reservationId);






    // ============================================
    // 미용사 전용 기능 
    // ============================================

    /**
     * 4. 내 미용실 예약 내역 가져오기 (미용사 예약 관리용)
     * 미용사(STYLIST)가 자신의 미용실에 들어온 모든 예약 요청을 조회합니다.
     *
     * @param userId 조회를 요청하는 미용사(STYLIST)의 ID
     * @return 미용사에게 들어온 예약 내역 목록
     */
    List<ReservationResponse> getStylistReservations(Long userId);




    /**
     * 5. 예약 상태 변경하기 (미용사용)
     * 미용사(STYLIST)가 들어온 예약의 상태를 승인(CONFIRMED), 완료(DONE), 또는 거절(CANCELLED)로 변경합니다.
     *
     * @param userId 상태를 변경하려는 미용사(STYLIST)의 ID
     * @param reservationId 상태를 변경할 예약 엔티티의 ID
     * @param status 변경할 상태값 (CONFIRMED, DONE, CANCELLED 등)
     */
    void updateReservationStatus(Long userId, Long reservationId, String status);
}
