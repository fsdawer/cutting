package beauty.beauty.auth.service;

import beauty.beauty.auth.dto.EmailRequest;
import beauty.beauty.auth.dto.LoginRequest;
import beauty.beauty.auth.dto.RegisterRequest;
import beauty.beauty.auth.dto.TokenResponse;
import beauty.beauty.global.exception.CustomException;
import beauty.beauty.global.exception.ErrorCode;
import beauty.beauty.global.jwt.JwtUtil;
import beauty.beauty.stylist.entity.Salon;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.repository.SalonRepository;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.user.entity.EmailVerification;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.EmailVerificationRepository;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final StylistProfileRepository stylistProfileRepository;
    private final SalonRepository salonRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    // ──────────────────────────────────────────────────────────
    // 회원가입
    // ──────────────────────────────────────────────────────────

    // Uesrname = id / 아이디와 이메일 중복 검증 -> 사용자 입력 값 저장 -> 이메일 전송
    @Override
    public void register(RegisterRequest request) {
        // 1. 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화 후 사용자 저장 (요청된 권한으로 설정)
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .role(User.Role.valueOf(request.getRole())) // 동적 권한 할당
                .provider(User.Provider.LOCAL)
                .isVerified(true)   // TODO: 배포 전 false로 변경 후 이메일 인증 활성화
                .build();
        userRepository.save(user);

        // STYLIST인 경우 미용실 + 프로필 저장
        if (User.Role.STYLIST.equals(user.getRole())) {
            Salon salon = salonRepository.save(Salon.builder()
                    .name(request.getSalonName())
                    .address(request.getLocation())
                    .phone(request.getSalonPhone())
                    .description(request.getSalonDescription())
                    .build());
            stylistProfileRepository.save(StylistProfile.builder()
                    .user(user)
                    .salon(salon)
                    .build());
        }

        // 3. 이메일 인증 메일 발송
        // TODO: 배포 전 아래 주석 해제 (isVerified=false 로 되돌릴 때 함께 활성화)
        // sendVerificationEmail(request.getEmail());
    }

    // ──────────────────────────────────────────────────────────
    // 로그인
    // ──────────────────────────────────────────────────────────

    @Override
    public TokenResponse login(LoginRequest request) {
        // 1. 사용자 조회 (username으로 먼저, 없으면 email로 시도)
        User user = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> userRepository.findByEmail(request.getUsername())
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS)));

        // 2. 비밀번호 검증
        // request안에 비빌먼호 요청값과 DB에 있는 비밀번호 값이 다르다면 오류
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 이메일 인증 여부 확인
        if (!user.isVerified()) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 4. JWT 발급 후 Refresh Token DB 저장
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        user.setRefreshToken(refreshToken);

        return new TokenResponse(accessToken, refreshToken, user.getRole().name());
    }

    // ──────────────────────────────────────────────────────────
    // 이메일 인증 코드 발송
    // ──────────────────────────────────────────────────────────

    @Override
    public void sendVerifyEmail(EmailRequest request) {
        sendVerificationEmail(request.getEmail());
    }

    /** 공통 내부 메서드: 인증 토큰 생성 & 메일 발송 */
    private void sendVerificationEmail(String email) {
        // 기존 미사용 인증 토큰 삭제 (중복 전송 방지)
        emailVerificationRepository.deleteByEmail(email);

        // UUID 기반 토큰 생성
        String token = UUID.randomUUID().toString().replace("-", "");

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .token(token)
                .expiredAt(LocalDateTime.now().plusMinutes(30)) // 30분 유효
                .isUsed(false)
                .build();
        emailVerificationRepository.save(verification);

        // 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Beauty] 이메일 인증");
        message.setText("아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n\n"
                + "http://localhost:8080/api/auth/email/verify?token=" + token
                + "\n\n링크는 30분간 유효합니다.");
        mailSender.send(message);
    }

    // ──────────────────────────────────────────────────────────
    // 이메일 인증 확인
    // ──────────────────────────────────────────────────────────

    @Override
    public void verifyEmail(String token) {
        // 1. 토큰 조회 (미사용만)
        EmailVerification verification = emailVerificationRepository
                .findByTokenAndIsUsedFalse(token)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        // 2. 만료 확인
        if (verification.isExpired()) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        // 3. 사용자 인증 완료 처리
        User user = userRepository.findByEmail(verification.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.setVerified(true);

        // 4. 토큰 사용 처리
        verification.setUsed(true);
    }

    // ──────────────────────────────────────────────────────────
    // 토큰 갱신
    // ──────────────────────────────────────────────────────────

    @Override
    public TokenResponse refresh(String refreshToken) {
        // 1. 서명 유효성 검사
        if (!jwtUtil.isValid(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. userId 추출 → 사용자 조회
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. DB에 저장된 토큰과 일치 여부 확인 (로그아웃된 사용자 차단)
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 4. Access Token + Refresh Token 재발급 (Rotation) 후 DB 갱신
        String newAccessToken  = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
        user.setRefreshToken(newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken, user.getRole().name());
    }

    // ──────────────────────────────────────────────────────────
    // 로그아웃
    // ──────────────────────────────────────────────────────────

    @Override
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.setRefreshToken(null);
    }
}
