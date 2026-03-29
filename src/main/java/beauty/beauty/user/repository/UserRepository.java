package beauty.beauty.user.repository;

import beauty.beauty.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // 아이디로 사용자 조회
    Optional<User> findByEmail(String email); // 이메일로 사용자 조회
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);
    boolean existsByUsername(String username); // 아이디가 존재하는지 체크
    boolean existsByEmail(String email);
}
