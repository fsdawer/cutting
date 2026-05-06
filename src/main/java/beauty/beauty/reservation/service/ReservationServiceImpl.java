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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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
    // [AFTER] 이벤트 기반 비동기 처리
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionTemplate transactionTemplate;

    private static final String UPLOAD_DIR = "uploads/reservation-images/";

    // ─────────────────────────────────────────────────────────────────────────
    // 1. 예약 생성  [AFTER — Redis SETNX 분산락 + 비동기 이벤트]
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @CacheEvict(value = "booked_times",
                key = "#request.stylistId + ':' + #request.reservedAt.toLocalDate().toString()")
    public ReservationResponse createReservation(Long userId, ReservationRequest request) {

        // [Flow 1] 예약 동시성 제어를 위한 Redis 분산락 획득 (Fail-Fast)
        // DB 커넥션을 점유하지 않은 상태(@Transactional 밖)에서 Redis 서버와 먼저 통신하여
        // 락을 획득하지 못하면 즉시 예외를 던지고 종료시킵니다. (커넥션 고갈 방지)
        String lockKey = "lock:reservation:" + request.getStylistId() + ":" + request.getReservedAt();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new IllegalStateException("해당 시간에는 이미 예약이 차있습니다. 다른 시간을 선택해주세요.");
        }

        try {
            // [Flow 2] 트랜잭션 바운더리 진입 (TransactionTemplate 적용)
            // 락을 무사히 획득한 스레드만 트랜잭션을 시작하며,
            // 최대한 짧은 시간 동안만 DB 커넥션을 꺼내 쓰고 즉시 반납합니다.
            return transactionTemplate.execute(status -> {
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

                OperatingHours hours = operatingHoursRepository
                        .findByStylistProfileIdAndDayOfWeek(stylist.getId(), dayOfWeekValue)
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

                // [Flow 3] 예약 정보 DB 저장
                // 여기서 DB INSERT 쿼리가 발생합니다.
                Reservation saved = reservationRepository.save(reservation);
                ChatRoom chatRoom = chatService.createRoomForReservation(saved);

                // [Flow 4] 이벤트 발행 (Redis Streams Publish)
                // DB 커밋 완료 후 Stream 발행 — 롤백 시 메시지가 남는 문제를 방지하기 위해 
                // TransactionSynchronizationManager.afterCommit() 시점에 발송합니다.
                long savedId = saved.getId();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 통신 오버헤드를 막기 위해 무거운 객체(Reservation) 대신 reservationId 단일 값만 던집니다.
                        stringRedisTemplate.opsForStream().add(
                                "reservation-events",
                                Map.of("reservationId", String.valueOf(savedId))
                        );
                        log.info("[Reservation] Stream 발행 완료 — reservationId={}", savedId);
                    }
                });

                log.info("[Reservation] 예약 생성 완료 — reservationId={}, stylistId={}, userId={}",
                        saved.getId(), stylist.getId(), userId);

                return ReservationResponse.from(saved, chatRoom.getId());
            });
        } finally {
            // [Flow 5] Redis 락 해제
            // DB 트랜잭션이 성공이든 예외든 완전히 끝난 직후 락을 반납하여 다른 스레드가 진입할 수 있게 합니다.
            redisTemplate.delete(lockKey);
        }
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
    // @CacheEvict(allEntries=true): 취소된 슬롯이 다시 비어야 하므로 캐시를 무효화해야 함
    // 그런데 캐시 키(stylistId:날짜)를 만들려면 reservationId로 DB를 먼저 읽어야 함
    // @CacheEvict는 메서드 실행 전에 키를 평가하므로 이 시점에 stylistId를 알 수 없음
    // → 어떤 키를 지워야 할지 모르니 booked_times 캐시 전체를 비움 (취소는 드문 작업이라 허용)
    @Override
    @Transactional
    @CacheEvict(value = "booked_times", allEntries = true)
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

        // 빈자리 알림 이벤트 발행 (Redis Streams)
        long stylistId = reservation.getStylistProfile().getId();
        LocalDate cancelDate = reservation.getReservedAt().toLocalDate();
        LocalTime cancelTime = reservation.getReservedAt().toLocalTime();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stringRedisTemplate.opsForStream().add(
                        "cancel_stream",
                        Map.of(
                                "stylistId", String.valueOf(stylistId),
                                "date", cancelDate.toString(),
                                "time", cancelTime.toString()
                        )
                );
                log.info("[Waiting] 예약 취소 발생, 빈자리 알림 이벤트 발행 — stylistId={}, datetime={}", stylistId, reservation.getReservedAt());
            }
        });
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
    // @Cacheable: 같은 미용사+날짜 조합은 30분간 Redis에서 바로 반환 (DB 조회 없음)
    // 캐시 키 예) "booked_times::1:2026-05-10" → ["10:00","11:00"] 저장
    // createReservation / cancelReservation 이 일어나면 해당 키를 삭제하므로 항상 최신 데이터
    @Override
    @Cacheable(value = "booked_times", key = "#stylistId + ':' + #date")
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
