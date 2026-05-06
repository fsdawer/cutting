package beauty.beauty.reservation.repository;

import beauty.beauty.reservation.entity.Waiting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {
    
    // 특정 미용사, 특정 날짜, 특정 시간에 대기 중인 고객 목록 조회 (페이징/Chunk 용)
    Page<Waiting> findByStylistProfileIdAndWaitingDateAndWaitingTime(
            Long stylistProfileId, LocalDate waitingDate, LocalTime waitingTime, Pageable pageable);
}
