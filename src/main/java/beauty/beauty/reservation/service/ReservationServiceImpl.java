package beauty.beauty.reservation.service;

import beauty.beauty.chat.entity.ChatRoom;
import beauty.beauty.chat.repository.ChatRoomRepository;
import beauty.beauty.chat.service.ChatServiceImpl;
import beauty.beauty.reservation.dto.ReservationRequest;
import beauty.beauty.reservation.dto.ReservationResponse;
import beauty.beauty.reservation.entity.Reservation;
import beauty.beauty.reservation.entity.ReservationImage;
import beauty.beauty.reservation.repository.ReservationImageRepository;
import beauty.beauty.reservation.repository.ReservationRepository;
import beauty.beauty.stylist.entity.OperatingHours;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.entity.StylistServiceItem;
import beauty.beauty.stylist.repository.OperatingHoursRepository;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.stylist.repository.StylistServiceRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final UserRepository userRepository;
    private final StylistProfileRepository stylistProfileRepository;
    private final StylistServiceRepository serviceRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationImageRepository reservationImageRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final ChatServiceImpl chatService;
    private final ChatRoomRepository chatRoomRepository;

    private static final String UPLOAD_DIR = "uploads/reservation-images/";

    // 1. 예약 생성
    @Override
    @Transactional
    public ReservationResponse createReservation(Long userId, ReservationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        StylistProfile stylist = stylistProfileRepository.findById(request.getStylistId())
                .orElseThrow(() -> new IllegalArgumentException("미용사를 찾을 수 없습니다."));

        StylistServiceItem stylistServiceItem = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 서비스를 찾을 수 없습니다."));

        if (!stylistServiceItem.getStylistProfile().getId().equals(stylist.getId())) {
            throw new IllegalArgumentException("해당 미용사가 제공하는 서비스가 아닙니다.");
        }

        LocalDateTime reservedAt = request.getReservedAt();
        int dayOfWeekValue = reservedAt.getDayOfWeek().getValue() - 1;
        LocalTime requestedTime = reservedAt.toLocalTime();

        OperatingHours hours = operatingHoursRepository.findByStylistProfileIdAndDayOfWeek(stylist.getId(), dayOfWeekValue)
                .orElseThrow(() -> new IllegalArgumentException("해당 요일의 영업시간 정보가 설정되지 않았습니다."));

        if (hours.isClosed()) {
            throw new IllegalArgumentException("해당 예약일은 휴무일입니다.");
        }

        if (requestedTime.isBefore(hours.getOpenTime()) || requestedTime.isAfter(hours.getCloseTime())) {
            throw new IllegalStateException("영업시간 외에는 예약할 수 없습니다. (영업시간: "
                    + hours.getOpenTime() + " ~ " + hours.getCloseTime() + ")");
        }

        boolean isBooked = reservationRepository.existsByStylistProfileIdAndReservedAtAndStatusIn(
                stylist.getId(),
                request.getReservedAt(),
                List.of(Reservation.Status.PENDING, Reservation.Status.CONFIRMED)
        );

        if (isBooked) {
            throw new IllegalStateException("해당 시간에는 이미 예약이 차있습니다. 다른 시간을 선택해주세요.");
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .stylistProfile(stylist)
                .service(stylistServiceItem)
                .reservedAt(request.getReservedAt())
                .status(Reservation.Status.CONFIRMED)
                .requestMemo(request.getRequestMemo())
                .totalPrice(stylistServiceItem.getPrice())
                .createdAt(LocalDateTime.now())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        ChatRoom chatRoom = chatService.createRoomForReservation(saved);
        return ReservationResponse.from(saved, chatRoom.getId());
    }

    // 2. 예약 이미지 업로드
    @Override
    @Transactional
    public void uploadImages(Long userId, Long reservationId, List<MultipartFile> files) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예약에만 이미지를 첨부할 수 있습니다.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String savedUrl = saveFile(file);
            reservationImageRepository.save(
                    ReservationImage.builder()
                            .reservation(reservation)
                            .imageUrl(savedUrl)
                            .build()
            );
        }
    }

    private String saveFile(MultipartFile file) {
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (ext != null ? "." + ext : "");
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/reservation-images/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }




    // 3. 내 예약 리스트 (마이페이지용)
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, Long> chatRoomIdMap = buildChatRoomIdMap(reservations);
        return reservations.stream()
                .map(r -> ReservationResponse.from(r, chatRoomIdMap.get(r.getId())))
                .toList();
    }



    // 4. 예약 상세 조회
    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));

        boolean isMyReservation = reservation.getUser().getId().equals(userId);
        boolean isMyStylistReservation = reservation.getStylistProfile().getUser().getId().equals(userId);

        if (!isMyReservation && !isMyStylistReservation) {
            throw new IllegalArgumentException("해당 예약 정보를 볼 권한이 없습니다.");
        }

        Long chatRoomId = chatRoomRepository.findByReservationId(reservationId)
                .map(ChatRoom::getId).orElse(null);
        return ReservationResponse.from(reservation, chatRoomId);
    }

    // 5. 예약 취소
    @Override
    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예약만 취소할 수 있습니다");
        }

        if (reservation.getStatus() != Reservation.Status.CONFIRMED
                && reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalArgumentException("대기 또는 확정 상태인 예약만 취소할 수 있습니다.");
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
        reservationRepository.save(reservation);
    }

    // 6. 미용사 예약 리스트 (예약 관리용)
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getStylistReservations(Long userId) {
        StylistProfile stylistProfile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("미용사 프로필이 없습니다"));

        List<Reservation> reservations = reservationRepository.findByStylistProfileId(stylistProfile.getId());
        Map<Long, Long> chatRoomIdMap = buildChatRoomIdMap(reservations);
        return reservations.stream()
                .map(r -> ReservationResponse.from(r, chatRoomIdMap.get(r.getId())))
                .toList();
    }

    // 7. 예약 상태 변경 (미용사용)
    @Override
    @Transactional
    public void updateReservationStatus(Long userId, Long reservationId, String status) {
        StylistProfile stylistProfile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("미용사 프로필이 없습니다"));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        if (!reservation.getStylistProfile().getId().equals(stylistProfile.getId())) {
            throw new IllegalArgumentException("본인에게 들어온 예약만 상태를 변경할 수 있습니다.");
        }

        Reservation.Status newStatus;
        try {
            newStatus = Reservation.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 예약 상태입니다.");
        }

        if (reservation.getStatus() == Reservation.Status.CANCELLED
                || reservation.getStatus() == Reservation.Status.DONE) {
            throw new IllegalStateException("이미 종료된 예약은 상태를 변경할 수 없습니다.");
        }

        reservation.setStatus(newStatus);
        if (newStatus == Reservation.Status.CONFIRMED) {
            chatService.createRoomForReservation(reservation);
        }
    }

    // 예약 목록 → reservationId:chatRoomId 맵 (N+1 방지용 배치 조회)
    private Map<Long, Long> buildChatRoomIdMap(List<Reservation> reservations) {
        if (reservations.isEmpty()) return Map.of();
        List<Long> ids = reservations.stream().map(Reservation::getId).toList();
        return chatRoomRepository.findByReservationIdIn(ids).stream()
                .collect(Collectors.toMap(cr -> cr.getReservation().getId(), ChatRoom::getId));
    }

    // 8. 특정 날짜 예약된 시간대 조회
    @Override
    public List<String> getStylistBookedTimes(Long stylistId, String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        LocalDateTime start = parsedDate.atStartOfDay();
        LocalDateTime end = parsedDate.atTime(LocalTime.MAX);

        List<Reservation.Status> validStatuses = List.of(
                Reservation.Status.PENDING, Reservation.Status.CONFIRMED
        );

        return reservationRepository
                .findByStylistProfileIdAndReservedAtBetweenAndStatusIn(stylistId, start, end, validStatuses)
                .stream()
                .map(r -> String.format("%02d:%02d", r.getReservedAt().getHour(), r.getReservedAt().getMinute()))
                .toList();
    }
}
