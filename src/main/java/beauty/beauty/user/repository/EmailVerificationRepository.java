package beauty.beauty.user.repository;

import beauty.beauty.user.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByTokenAndIsUsedFalse(String token);
    void deleteByEmail(String email);
}
