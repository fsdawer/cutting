package beauty.beauty.reservation.service;

import beauty.beauty.chat.service.ChatService;
import beauty.beauty.reservation.dto.ReservationRequest;
import beauty.beauty.reservation.dto.ReservationResponse;
import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.reservation.repository.ReservationRepository;
import beauty.beauty.stylist.entity.OperatingHours;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.entity.StylistServiceItem;
import beauty.beauty.stylist.repository.OperatingHoursRepository;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.stylist.repository.StylistServiceRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {


    private final UserRepository userRepository;
    private final StylistProfileRepository stylistProfileRepository;
    private final StylistServiceRepository serviceRepository;
    private final ReservationRepository reservationRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    // 채팅방 생성을 위해 ChatService 주입
    private final ChatService chatService;


    // 1. 예약 생성하기 (일반 사용자 -> 미용사)
    // 유저인지 검증 -> DB에 있는 미용사인지 검증 -> 시술이 있는지 검증 -> 요청한 시술이 해당 미용사에 있는 시술인지 검증 -> 영업시간 조회 -> 휴무일 검증
    // -> 영업시간보다 빠르거나 끝났을때 예약 했는지 검증 -> 해당 미용사의 해당 시간에 이미 예약이 있는지 검증
    @Override
    @Transactional
    public void createReservation(Long userId, ReservationRequest request) {
        // 1. 엔티티 조회
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // DB에 있는 스타일리스트 아이디랑 request안에 있는 스타일리스트 아이디가 같은지 비교
        StylistProfile stylist = stylistProfileRepository.findById(request.getStylistId())
                .orElseThrow(() -> new IllegalArgumentException("미용사를 찾을 수 없습니다."));

        // DB에 있는 서비스와 request안에 있는 서비스가 있는지 검증
        StylistServiceItem stylistServiceItem = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 서비스를 찾을 수 없습니다."));

        // 요청한 서비스가 해당 미용사의 서비스가 맞는지 확인
        // 해당 서비스를 가지고 있는 미용사의 아이디와 DB에 있는 미용사 아이디와 다르다면
        if(!stylistServiceItem.getStylistProfile().getId().equals(stylist.getId())) {
            throw new IllegalArgumentException("해당 미용사가 제공하는 서비스가 아닙니다.");
        }

        // 예약 시간에서 '요일'과 '시간'만 추출
        LocalDateTime reservedAt = request.getReservedAt();

        int dayOfWeekValue = reservedAt.getDayOfWeek().getValue() - 1;
        LocalTime requestedTime = reservedAt.toLocalTime();

        // 해당 미용사의 해당 요일 영업시간 정보 가져오기
        OperatingHours hours = operatingHoursRepository.findByStylistProfileIdAndDayOfWeek(stylist.getId(), dayOfWeekValue)
                .orElseThrow(() -> new IllegalArgumentException("해당 요일의 영업시간 정보가 설정되지 않았습니다."));

        // 가져온 영업시간에서 휴무일인지 확인
        if(hours.isClosed()) {
            throw new IllegalArgumentException("해당 예약일은 휴무일입니다.");
        }

        // 예약한 시간이 영업시간보다 빠르거나 종료시간보다 늦은지 체크
        if(requestedTime.isBefore(hours.getOpenTime()) || requestedTime.isAfter(hours.getCloseTime())) {
            throw new IllegalStateException("영업시간 외에는 예약할 수 없습니다. (영업시간: "
                    + hours.getOpenTime() + " ~ " + hours.getCloseTime() + ")");
        }

        // 해당 미용사의 해당 시간에 이미 CONFIRMED 예약이 있는지 검증
        boolean isBooked = reservationRepository.existsByStylistProfileIdAndReservedAtAndStatusIn(
                stylist.getId(),
                request.getReservedAt(),
                List.of(Reservation.Status.CONFIRMED)
        );

        if (isBooked) {
            throw new IllegalStateException("해당 시간에는 이미 예약이 차있습니다. 다른 시간을 선택해주세요.");
        }

        // 예약 생성 (즉시 확정 상태로 저장)
        Reservation reservation = Reservation.builder()
                .user(user)
                .stylistProfile(stylist)
                .service(stylistServiceItem)
                .reservedAt(request.getReservedAt())
                .status(Reservation.Status.CONFIRMED) // 예약 즉시 확정
                .requestMemo(request.getRequestMemo())
                .totalPrice(stylistServiceItem.getPrice())
                .build();
        reservationRepository.save(reservation);

        // 4. 예약 저장이 완료되자마자 곧바로 채팅방 생성 기능 호출
        // 채팅방 완성 후 호출
    }



    // 2. 내 예약 리스트 가져오기 (마이페이지용)
    @Override
    public List<ReservationResponse> getMyReservations(Long userId) {
        // 1. 예약 레포지토리에서 내 ID로 된 모든 예약(Reservation 엔티티)을 시간 역순 등으로 가져옵니다.
        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        // 2. 엔티티 리스트를 프론트엔드가 요구하는 DTO(ReservationResponse) 리스트로 변환합니다.
        return reservations.stream()
                .map(reservation -> {
                    Long chatRoomId = null;
                    return ReservationResponse.from(reservation, chatRoomId);
                })
                .toList();
    }

    // 내 예약 상세 조회
    @Override
    public ReservationResponse getReservationById(Long userId, Long reservationId) {

        // 1. 예약 ID로 예약 엔티티 1건 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));

        // 예약 엔티티에 연관된 User 객체를 가져와서 그 userId와 요청값에 있는 userId가 같은지 검증 -> 내 예약인지 검증
        boolean isMyReservation = reservation.getUser().getId().equals(userId);

        // 예약 엔터티에 연관된 스타일리스트 객체를 가져와서 그 userId와 요청값에 있는 userId가 같은지 검증 -> 내가 예약한 미용사인지 검증
        boolean isMyStylistReservation = reservation.getStylistProfile().getUser().getId().equals(userId);

        if(!isMyReservation && !isMyStylistReservation) {
            throw new IllegalArgumentException("해당 예약 정보를 볼 권한이 없습니다.");
        }

        // 3. DTO로 변환해서 리턴
        return ReservationResponse.from(reservation, null);
    }



    // 3. 예약 취소하기
    @Override
    public void cancelReservation(Long userId, Long reservationId) {

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));

        // 내 예약이 맞는지 확인
        if(!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예약만 취소할 수 있습니다");
        }

        // CONFIRMED(확정) 상태인 예약만 취소 가능
        if(reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("확정(CONFIRMED) 상태인 예약만 취소할 수 있습니다.");
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
    }



    // 특정 미용사의 예약 리스트 가져오기 (미용사 예약 관리용)
    @Override
    public List<ReservationResponse> getStylistReservations(Long userId) {

        // 이 유저가 미용사가 맞는지 확인
        StylistProfile stylistProfile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("미용사 프로필이 없습니다"));

        // 2. 해당 미용사 ID(stylistProfile.getId())로 들어온 모든 예약 조회
        List<Reservation> reservationList = reservationRepository.findByStylistProfileId(stylistProfile.getId());

        // 3. DTO 리스트로 변환해서 리턴 (위 사용자용 조회와 똑같은 코드)
        return reservationList.stream()
                .map(reservation -> ReservationResponse.from(reservation, null))
                .toList();
    }


    // 5. 예약 상태 변경하기 (미용사용)
    @Override
    @Transactional
    public void updateReservationStatus(Long userId, Long reservationId, String status) {
        // 이 유저가 미용사가 맞는지 확인, 미용사 프로필 조회
        StylistProfile stylistProfile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("미용사 프로필이 없습니다"));

        // 2. 예약 내역 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 3. 해당 예약이 현재 미용사(본인)에게 들어온 예약이 맞는지 권한 검증
        if (!reservation.getStylistProfile().getId().equals(stylistProfile.getId())) {
            throw new IllegalArgumentException("본인에게 들어온 예약만 상태를 변경할 권한이 없습니다.");
        }

        // 4. 프론트엔드에서 넘어온 문자열 상태값(String)을 Enum으로 변환
        Reservation.Status newStatus;
        try {
            newStatus = Reservation.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            // PENDING, CONFIRMED, DONE, CANCELLED 외의 문자가 들어올 경우 방어
            throw new IllegalArgumentException("유효하지 않은 예약 상태입니다.");
        }

        // 이미 완료/취소된 예약인 경우 변경 불가 방어
        if (reservation.getStatus() == Reservation.Status.CANCELLED ||
                reservation.getStatus() == Reservation.Status.DONE) {
            throw new IllegalStateException("이미 진행이 종료된(취소/완료) 예약은 상태를 변경할 수 없습니다.");
        }

        // 6. DB 상태 업데이트
        reservation.setStatus(newStatus);

        
    }
}
