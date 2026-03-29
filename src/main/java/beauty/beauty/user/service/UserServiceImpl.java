package beauty.beauty.user.service;

import beauty.beauty.user.dto.ChangePasswordRequest;
import beauty.beauty.user.dto.UpdateUserRequest;
import beauty.beauty.user.dto.UpgradeToStylistRequest;
import beauty.beauty.user.dto.UserResponse;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final StylistProfileRepository stylistProfileRepository;
    private final PasswordEncoder passwordEncoder;



    // GET  /api/users/me               내 정보 조회
    @Override
    public UserResponse selectMyInformation(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }



    // PUT  /api/users/me 내 정보 수정
    @Override
    @Transactional
    public UserResponse changeMyInformation(Long userId, UpdateUserRequest updateUserRequest) {
        User user = findUserById(userId);

        // null이 아닌 필드만 선택적으로 업데이트
        // 업데이트된 이름을 가져와서 user.setname한다
        if (updateUserRequest.getName() != null) {
            user.setName(updateUserRequest.getName());
        }
        if (updateUserRequest.getPhone() != null) {
            user.setPhone(updateUserRequest.getPhone());
        }
        if (updateUserRequest.getProfileImg() != null) {
            user.setProfileImg(updateUserRequest.getProfileImg());
        }

        return UserResponse.from(user);
    }

    // PUT  /api/users/me/password      비밀번호 변경
    @Override
    @Transactional
    public void changeMyPassword(Long userId, ChangePasswordRequest changePasswordRequest) {
        User user = findUserById(userId);

        // 1. 비밀변호 변경하기 위해 내 비밀번호를 입력한 데이터 = dto.getCurrentPassword
        // -> 입력한 비밀번호와 DB에 있는 비밀번호와 같은지 값 비교 = user.getPassword
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 2. 새 비밀번호와 새 비밀번호 확인이 일치하는지 확인
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        // 3. 새 비밀번호를 BCrypt로 해싱 후 저장
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
    }

    @Override
    @Transactional
    public void deleteMe(Long userId) {
        User user = findUserById(userId);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse upgradeToStylist(Long userId, UpgradeToStylistRequest request) {
        User user = findUserById(userId);

        if (user.getRole() == User.Role.STYLIST) {
            throw new IllegalArgumentException("이미 미용사(STYLIST) 권한을 가지고 있습니다.");
        }

        // 1. 권한 변경
        user.setRole(User.Role.STYLIST);

        // 2. 미용사 프로필 생성
        StylistProfile profile = StylistProfile.builder()
                .user(user)
                .salonName(request.getSalonName())
                .location(request.getLocation())
                .build();
        stylistProfileRepository.save(profile);

        return UserResponse.from(user);
    }




    // ID 조회 공통 메서드
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저입니다. id=" + userId));
    }
}
