package beauty.beauty.user.dto;

import beauty.beauty.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private Long id;
    private String username;        // 로그인 아이디
    private String email;
    private String name;
    private String phone;
    private String role;            // USER or STYLIST
    private String profileImg;      // 프로필 이미지 URL
    private String provider;        // LOCAL, KAKAO, NAVER
    private boolean isVerified;     // 이메일 인증 여부
    private LocalDateTime createdAt;

    // Entity → DTO 변환 (정적 팩토리 메서드)
    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.id         = user.getId();
        dto.username   = user.getUsername();
        dto.email      = user.getEmail();
        dto.name       = user.getName();
        dto.phone      = user.getPhone();
        dto.role       = user.getRole().name();
        dto.profileImg = user.getProfileImg();
        dto.provider   = user.getProvider().name();
        dto.isVerified = user.isVerified();
        dto.createdAt  = user.getCreatedAt();
        return dto;
    }
}
