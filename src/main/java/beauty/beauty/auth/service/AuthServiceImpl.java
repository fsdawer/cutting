package beauty.beauty.auth.service;

import beauty.beauty.auth.dto.LoginRequest;
import beauty.beauty.auth.dto.RegisterRequest;
import beauty.beauty.auth.dto.TokenResponse;
import beauty.beauty.global.exception.CustomException;
import beauty.beauty.global.exception.ErrorCode;
import beauty.beauty.global.jwt.JwtUtil;
import beauty.beauty.global.redis.TokenBlacklistService;
import beauty.beauty.stylist.entity.Salon;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.repository.SalonRepository;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StylistProfileRepository stylistProfileRepository;
    private final SalonRepository salonRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .role(User.Role.valueOf(request.getRole()))
                .provider(User.Provider.LOCAL)
                .isVerified(true)
                .build();
        userRepository.save(user);

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
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        user.setRefreshToken(refreshToken);

        return new TokenResponse(accessToken, refreshToken, user.getRole().name());
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String newAccessToken  = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
        user.setRefreshToken(newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken, user.getRole().name());
    }

    @Override
    public void logout(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.setRefreshToken(null);

        if (accessToken != null && jwtUtil.isValid(accessToken)) {
            tokenBlacklistService.blacklist(accessToken, jwtUtil.getExpiration(accessToken));
        }
    }
}
