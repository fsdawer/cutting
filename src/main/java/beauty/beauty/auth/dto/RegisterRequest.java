package beauty.beauty.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;      // 로그인 아이디

    @NotBlank @Email
    private String email;        // 이메일 인증

    @NotBlank
    @Size(min = 8)
    private String password;    // 비밀번호

    @NotBlank
    @Size(max = 50)
    private String name;          // 표시 이름

    private String phone;       // 핸드폰 번호

    @NotBlank
    private String role;        // 역할 (USER 또는 STYLIST)

    // STYLIST인 경우 입력받는 추가 정보
    private String salonName;        // 미용실 이름
    private String location;         // 미용실 주소
    private String salonPhone;       // 미용실 전화번호 (선택)
    private String salonDescription; // 미용실 소개 (선택)
}
