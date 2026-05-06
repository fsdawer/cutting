package beauty.beauty.reservation.service;

import beauty.beauty.reservation.entity.Waiting;
import beauty.beauty.reservation.repository.WaitingRepository;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class WaitingService {

    private final WaitingRepository waitingRepository;
    private final UserRepository userRepository;
    private final StylistProfileRepository stylistProfileRepository;

    @Transactional
    public void registerWaiting(Long userId, Long stylistProfileId, LocalDate date, LocalTime time) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        StylistProfile stylistProfile = stylistProfileRepository.findById(stylistProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Stylist not found"));

        Waiting waiting = Waiting.builder()
                .user(user)
                .stylistProfile(stylistProfile)
                .waitingDate(date)
                .waitingTime(time)
                .build();
        
        waitingRepository.save(waiting);
    }
}
